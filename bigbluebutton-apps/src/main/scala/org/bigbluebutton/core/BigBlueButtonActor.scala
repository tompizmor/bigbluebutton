package org.bigbluebutton.core

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.HashMap
import org.bigbluebutton.core.api.CreateMeeting
import org.bigbluebutton.core.api.MeetingCreated
import org.bigbluebutton.core.api.MessageOutGateway
import org.bigbluebutton.core.api.InMessage
import org.bigbluebutton.core.api.InitializeMeeting
import org.bigbluebutton.core.api.DestroyMeeting
import org.bigbluebutton.core.api.KeepAliveMessage
import org.bigbluebutton.core.api.KeepAliveMessageReply

import org.red5.logging.Red5LoggerFactory
import org.slf4j.Logger

class BigBlueButtonActor(outGW: MessageOutGateway) extends Actor {
  private var log = Red5LoggerFactory.getLogger(classOf[BigBlueButtonActor], "bigbluebutton")

  private var meetings = new HashMap[String, Meeting]
 
  def act() = {

	loop {
		react {
	      case createMeeting: CreateMeeting => handleCreateMeeting(createMeeting)
	      case destroyMeeting: DestroyMeeting => handleDestroyMeeting(destroyMeeting)
	      case KeepAliveMessage => handleKeepAliveMessage()
	      case msg:InMessage => handleMeetingMessage(msg)
	      case _ => // do nothing
	    }
	}
  }
  
  private def handleMeetingMessage(msg: InMessage):Unit = {
    println("receiving:" + msg)
    meetings.get(msg.meetingID) match {
      case None => // do nothing
      case Some(m) => m ! msg
    }
  }

  private def handleKeepAliveMessage():Unit = {
    println("receiving keep alive message")
    outGW.send(new KeepAliveMessageReply())
  }
  
  private def handleDestroyMeeting(msg: DestroyMeeting) {
    meetings.get(msg.meetingID) match {
      case None => // do nothing
      case Some(m) => {
        m ! StopMeetingActor
        meetings -= msg.meetingID
      }
    }    
  }
  
  private def handleCreateMeeting(msg: CreateMeeting):Unit = {
    meetings.get(msg.meetingID) match {
      case None => {
    	  var m = new Meeting(msg.meetingID, msg.recorded, msg.voiceBridge, outGW)
    	  m.start
    	  meetings += m.meetingID -> m
    	  outGW.send(new MeetingCreated(m.meetingID, m.recorded))
    	  
    	  m ! new InitializeMeeting(m.meetingID, m.recorded)
      }
      case Some(m) => // do nothing
    }
  }
  
}