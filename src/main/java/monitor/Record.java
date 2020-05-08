package monitor;

public class Record {
    private long time;
    private String content;

    public Record(long time, String content) {
        this.time = time;
        this.content = content;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTime() {
        return this.time;
    }

    public String getContent() {
        return this.content;
    }
}