/*
 * Copyright 2003, 2005 Neil McKellar and Chris Dailey
 * All rights reserved.
 */
package org.intranet.sim.event;

import java.util.Comparator;

import au.edu.rmit.agtgrp.elevatorsim.Transmittable;

/**
 * @author Neil McKellar and Chris Dailey
 *
 */
public abstract class Event implements Transmittable
{
  private static long maxId;
  private long id;
  private long time;
  
  private Event()
  {
    super();
  }
  
  public Event(long newTime)
  {
    super();
    id = getNextId();
    time = newTime;
  }
  
  private static synchronized long getNextId()
  {
    return maxId++;
  }
  
  public long getId()
  {
    return id;
  }
  
  public String toString()
  {
    String fullClassName = getClass().getName();
    String shortClassName =
        fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    return  shortClassName + ": " + Long.toString(time);
  }
  
  public long getTime()
  {
    return time;
  }
  
  public abstract void perform();
  
  public static final class EventTimeComparator
    implements Comparator<Event>
  {
    public int compare(Event e1, Event e2)
    {
      long diff = (e1.getTime() - e2.getTime());
      if (diff == 0)
      {
        long idDiff = e1.getId() - e2.getId();
        return idDiff == 0 ? 0 : idDiff > 0 ? 1 : -1;
      }
      return (diff > 0) ? 1 : -1;
    }
  }
}
