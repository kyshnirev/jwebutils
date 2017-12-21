# jwebutils
Utils for Web writen in java

### GetVideoInfo 

Allows to get URL for downloading video from YouTube

#### compile
    javac -d bin src/main/java/kyshnirev/webutil/youtube/GetVideoInfo.java
    
#### run
    java -cp bin/ kyshnirev.webutil.youtube.GetVideoInfo <videId>

#### usage
    GetVideoInfo getiv = new GetVideoInfo("h123b17");
    getiv.fetch();

    System.out.println("fetch "+ getiv.getVideos().size() + " videos");

    for (VideoInfo vi : getiv.getVideos()) {
      System.out.printf("%-5s | %-32s | %s \n", vi.quality_label, vi.type, vi.url);
    }