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

package ag.bett.lift.bootstrap.snippet

import ag.bett.lift.bootstrap.model._
import ag.bett.lift.bootstrap.comet._
import ag.bett.lift.bootstrap.lib._

import ag.bett.lift.bhtml._

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._

import scala.xml._
import java.util.{Date, Calendar}


class Users extends Loggable {

	lazy val isAdmin_? = User.superUser_?
	lazy val isPrivileged_? = User.can_?(Role.Administrator, Role.Accounting, Role.Support)
	lazy val isUser_? = User.loggedIn_?

	def jsLogin(a: String): JsCmd = {
		def loginFailed = JsRaw("""
			$('.loginerror').fadeIn(1000);
			if ($('#login_user').val().length > 0) {
				$('#login_pass').select();
			} else {
				$('#login_user').select();
			}
			window.setTimeout(function() { $('.loginerror').fadeOut(1000); }, 3000);
		""".replaceAll("(\\s\\s+|\r|\n)", " "))

		val splitted = a.split("--## ##--")
		if (splitted.length != 2) return loginFailed
		if (User.login(splitted.head, splitted.last))
			RedirectTo(User.loginRedirect.get openOr "/")
		else loginFailed
	}

	/* snippets */
	def isadmin(xhtml: NodeSeq) = if (isAdmin_?) xhtml else NodeSeq.Empty
	def isnotadmin(xhtml: NodeSeq) = if (!isAdmin_?) xhtml else NodeSeq.Empty

	def isprivileged(xhtml: NodeSeq) = if (isPrivileged_?) xhtml else NodeSeq.Empty
	def isnotprivileged(xhtml: NodeSeq) = if (!isPrivileged_?) xhtml else NodeSeq.Empty

	def isloggedin(xhtml: NodeSeq) = if (isUser_?) xhtml else NodeSeq.Empty
	def isnotloggedin(xhtml: NodeSeq) = if (!isUser_?) xhtml else NodeSeq.Empty

	/** Displays passed NodeSeq only if there are <b>NO</b> superusers in the system */
	def nosuperuser(xhtml: NodeSeq) = if (User.findAll(By(User.superUser, true)).length == 0) xhtml else NodeSeq.Empty

	def loggedinas = User.currentUser match {
		case Full(u) => "a *" #> u.email.is
		case _ => "*" #> "" // Empty out
	}
	
	private val loginJsSubmit = JsRaw("""($('#login_user').val() + '--## ##--' + $('#login_pass').val()).replace('\"|\'', '')""")
	def login = "form [onsubmit]" #> (SHtml.ajaxCall(loginJsSubmit, jsLogin)._2 & JsRaw("return false")).toJsCmd

	private def makeSuper: JsCmd = User.currentUser match {
		case Full(u) =>
			u.superUser(true).roles(List(Role.Administrator)).save
			JsRaw("""window.location.reload()""")
		case _ => Noop
	}

	def becomesuperuser(xhtml: NodeSeq): NodeSeq = {
		<button class="btn primary" onclick={SHtml.ajaxInvoke(() => makeSuper)._2.toJsCmd}>
			<lift:loc locid="become.superuser">Become Super User</lift:loc>
		</button>
	}

}


class SendNotification extends StatefulSnippet {

	def dispatch = {
		case "render" => render
	}

	var title = "Admin Message"
	var text = ""
	var customer = ""
	var img = ""
	var sticky = false

	def notification = NotificationMessage(
		title,
		text.replaceAll("\r?\n", "<br/>"),
		img match { case "" => Empty case a: String => Full(a) },
		sticky,
		Empty,
		if (customer == "") Empty else Full(customer.toLong),
		List())

	def render: CssSel =
		".notification_title" #> SHtml.ajaxText(title, (v: String) => { title = v; Noop }) &
		".notification_text" #> SHtml.ajaxTextarea(text, (v: String) => { text = v; Noop }) &
		".notification_sticky" #> SHtml.ajaxCheckbox(sticky, (v: Boolean) => { sticky = v; Noop }) &
		".notification_customer" #> SHtml.ajaxText(customer, (v: String) => { customer = v; Noop }) &
		".notification_image" #> SHtml.ajaxTextarea(img, (v: String) => { img = v; Noop }) &
		".notification_send [onclick]" #> SHtml.ajaxInvoke(() => { NotificationCenter ! notification; Noop })

}


