package io.github.andreaszks.speedportplusphonecalls;

/** A POJO that represents a phone call. */
public class PhoneCall {

  public final String id;
  public final String date;
  public final String time;
  public final String who;
  public final String port;
  public final String duration; // "NO_ANSWER" if missed, numeric if taken/dialed

  PhoneCall(String id, String date, String time, String who, String port, String duration) {
    this.id = id;
    this.date = date;
    this.time = time;
    this.who = who;
    this.port = port;
    this.duration = duration;
  }

  public String toCsv() {
    return String.join(",", id, date, time, who, port, duration);
  }
}
