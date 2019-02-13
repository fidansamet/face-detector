package net.fidansamet.facedetector;

import android.content.Context;

public class PersonLine {

    private String person;
    private Context context;

    public PersonLine (String person) {
        super();
        this.person = person;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) { this.person = person; }

}
