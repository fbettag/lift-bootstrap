/* {{{
 *  Copyright 2012 Franz Bettag <franz@bett.ag>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/// }}}

package ag.bett.lift.bootstrap.comet

import ag.bett.lift.bootstrap.model._
import ag.bett.lift.bootstrap.lib._

import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.SHtml._
import net.liftweb.util._

import scala.collection.mutable.HashMap
import java.util.Date


case class NotificationMessage(title: String, text: String, image: Box[String], sticky: Boolean, time: Box[Int], customer: Box[Long], role: List[Role.Value]) {

	private val creationDate = new Date
	def date = DateTimeHelpers.getDate(creationDate)

	def toJsCmd = JsRaw("""
		$.gritter.add({
			// (string | mandatory) the heading of the notification
			title: "%s",
			// (string | mandatory) the text inside the notification
			text: "%s",
			// (string | optional) the image to display on the left
			image: "%s",
			// (bool | optional) if you want it to fade out on its own or just sit there
			sticky: %s,
			// (int | optional) the time you want it to be alive for before fading out
			time: %s,
			// (string | optional) the class name you want to apply to that specific message
			class_name: 'klog-sticky'
		})
	""".format(
		title.replaceAll("\"", "\\\""),
		text.replaceAll("\"", "\\\""),
		image match { case Full(a) => a case _ => "" },
		sticky,
		time match { case Full(a) => a case _ => "''" }))

}


object NotificationCenter extends LiftActor with ListenerManager {

	def createUpdate = "dummy"

	override def lowPriority = {
		case s: NotificationMessage => updateListeners(s)
	}

}


class UserNotification extends CometActor with CometListener {

	def registerWith = NotificationCenter

	private var msgs: List[NotificationMessage] = List()
	
	override def lowPriority = {
		case v: NotificationMessage => v.customer match {
			case Full(c) => if (c == User.currentUser.open_!.customer.is) submit(v)
			case Empty => submit(v)
			case _ =>
		}
	}

	private def submit(v: NotificationMessage) {
		v :: msgs
		partialUpdate(v.toJsCmd)
	}

	def render = "*" #> ""

}
