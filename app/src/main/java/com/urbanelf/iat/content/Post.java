package com.urbanelf.iat.content;

public class Post {
    private final String author;
    private final String content;

    public Post(String author, String content) {
        this.author = author;
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }
}
