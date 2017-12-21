package kyshnirev.webutil.youtube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

/**
 * Allows to get URL for downloading video from YouTube.<br>
 * Make <code>get_video_info</code> request and parse video URLs.<br>
 * <br>
 * Do not forgot call {@link #fetch()} method to perform request and parse results.<br>
 * <br>
 * Dec 2017
 */
public class GetVideoInfo {

  /**
   * Allows to run from command line and print result to stdout.<br>
   * @param args - videoId
   */
  public static void main(String[] args) {
    String videoId = null;

    // get videoId from args
    if (args.length >= 1) {
      videoId = args[0];
    }

    // read video id from stdin
    if (videoId == null) {
      out.print("input video id: ");
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      try {
        videoId = stdin.readLine();
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
    }

    out.println("video id = "+ videoId);

    GetVideoInfo getiv = new GetVideoInfo(videoId);
    getiv.fetch();

    System.out.println("fetch "+ getiv.getVideos().size() + " videos");

    for (VideoInfo vi : getiv.getVideos()) {
      System.out.printf("%-5s | %-32s | %s \n", vi.quality_label, vi.type, vi.url);
    }
  }

  private static final String URL_TEMPLATE = "https://www.youtube.com/get_video_info?video_id=%s";
  private static final String ADAPTIVE_FMTS = "adaptive_fmts";

  private final String videoId;

  private List<VideoInfo> videos = Collections.emptyList();

  public GetVideoInfo(String videoId) {
    if (notEmpty(videoId)) {
      this.videoId = videoId;
    } else {
      throw new IllegalArgumentException("videoId missing");
    }
  }

  public List<VideoInfo> getVideos() {
    return videos;
  }

  /**
   * Download video info from youtube and parse it
   *
   * @return this
   */
  public GetVideoInfo fetch() {
    final String href = String.format(URL_TEMPLATE, videoId);
    final URL url;
    try {
      url = new URL(href);
    } catch (MalformedURLException e) {
      throw new RuntimeException("bad url: " + href, e);
    }

    final String text;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
      text = sb.toString();
    } catch (IOException e) {
      throw new RuntimeException("failed to download video info", e);
    }

    {
      System.out.println(">> text len = "+ text.length());

      int p = text.indexOf(ADAPTIVE_FMTS);
      System.out.println(">> adaptive_fmts position = "+ p);

      System.out.println(">> " +text.substring(p, p + 80));
    }

    parse(text);

    return this;
  }

  /**
   * Parse video info from x-www-form-ulrencoded string
   *
   * @param text -- key=value&key=value
   */
  private void parse(String text) {

    Map<String, String> params = decodeParams(text);
    String encodedFmts = params.get(ADAPTIVE_FMTS);
    if (encodedFmts == null) {
      throw new RuntimeException("failed find '"+ ADAPTIVE_FMTS +"': " + text);
    }

    // x-www-form-ulrencoded string with list of videos
    String adaptiveFmts = decodeUrl(encodedFmts);
    if (adaptiveFmts == null || adaptiveFmts.isEmpty()) {
      throw new RuntimeException("failed to decode '"+ ADAPTIVE_FMTS +"': " + encodedFmts);
    }

    // array with structure x-form-url encoded
    String viStrs[] = adaptiveFmts.split(",");
    if (viStrs.length == 0) {
      throw new RuntimeException("no video info in '" + adaptiveFmts + "'");
    }

    // decode each video info structure
    List<VideoInfo> viList = new ArrayList<>(viStrs.length);
    for (String viStr : viStrs) {
      //System.out.println("decode VI: "+ viStr);

      Map<String, String> viParams = decodeParams(viStr);
      VideoInfo vi = new VideoInfo(viParams);
      if (vi.isValid()) {
        viList.add(vi);
      } else {
        //System.out.println("Skip invalid video struct");
        //System.out.println("  "+ vi);
        throw new RuntimeException("failed to decode VideoInfo struct from param string: '" + viStr + "'");
      }
    }

    this.videos = viList;
  }

  /**
   * Try to decode text as x-www-form-urlencoded, if failed return null
   *
   * @param text
   * @return null if decode failed
   */
  static String decodeUrl(String text) {
    if (text == null) {
      return null;
    }
    try {
      return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  private Map<String, String> decodeParams(String urlEncoded) {
    Map<String, String> map = new HashMap<>();
    for (String param : urlEncoded.split("&")) {
      String pp[] = param.split("=");
      if (pp.length != 2) {
        continue;
      }
      String k = pp[0];
      String v = pp[1];
      if (map.put(k, v) != null) {
        // NOTE: handle several values for one key
      }
    }
    return map;
  }

  private static boolean notEmpty(String s) {
    return s != null && !s.trim().isEmpty();
  }

  public static class VideoInfo {
    public final String type;            // video/mp4; codecs="avc1.4d401e"
    public final String bitrate;         // 990134
    public final String fps;             // 30
    public final String lmt;             // 1417810479193972
    public final String projection_type; // 1
    public final String clen;            // 92032447
    public final String init;            // 0-707
    public final String itag;            // 135
    public final String index;           // 708-3031
    public final String xtags;           //
    public final String quality_label;   // 480p
    public final String size;            // 640x480
    public final String url;             // https://r3---sn-npcnxu-v8ce.googlevideo.com/videoplayback?...

    private VideoInfo(Map<String, String> params) {
      this.type = decodeUrl(params.get("type"));
      this.bitrate = params.get("bitrate");
      this.fps = params.get("fps");
      this.lmt = params.get("lmt");
      this.projection_type = params.get("projection_type");
      this.clen = params.get("clen");
      this.init = params.get("init");
      this.itag = params.get("itag");
      this.index = params.get("index");
      this.xtags = params.get("xtags");
      this.quality_label = params.get("quality_label");
      this.size = params.get("size");
      this.url = decodeUrl(params.get("url"));
    }

    private boolean isValid() {
      return notEmpty(type) && notEmpty(url);
    }

    @Override
    public String toString() {
      return String.format(
          "%s{type=%s, bitrate=%s, fps=%s, lmt=%s, projection_type=%s, clen=%s, init=%s, itag=%s, index=%s, xtags=%s, quality_label=%s, size=%s, url=%s}",
          getClass().getSimpleName(),
          type, bitrate, fps, lmt, projection_type, clen, init, itag, index, xtags, quality_label, size, url
      );
    }

  } // :~

}
