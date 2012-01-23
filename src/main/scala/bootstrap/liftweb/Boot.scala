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

package bootstrap.liftweb

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.http._
import net.liftweb.util._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

import ag.bett.lift.bhtml._

import ag.bett.lift.bootstrap.model._
import ag.bett.lift.bootstrap.actors._

import java.sql.{Connection, DriverManager}
import java.net._


class Boot {
	def boot {

		if (!DB.jndiJdbcConnAvailable_?) {
			val main = new StandardDBVendor(Props.get("main.driver") openOr "org.h2.Driver",
				Props.get("main.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
				Props.get("main.user"), Props.get("main.password"))

			LiftRules.unloadHooks.append(main.closeAllConnections_! _)
			DB.defineConnectionManager(DefaultConnectionIdentifier, main)

			Schemifier.schemify(true, Schemifier.infoF _, DefaultConnectionIdentifier, User, Customer)

		}

		System.setProperty("mail.smtp.host", Props.get("mail.smtp.host") openOr "localhost")
		System.setProperty("mail.smtp.from", Props.get("mail.smtp.from") openOr "noreply@i.didnt.configure.jack.shit")


		/* Actors */
		CustomerCache.init

		// Where to search snippet
		LiftRules.addToPackages("ag.bett.lift.bootstrap")
		LiftRules.explicitlyParsedSuffixes = Set("htm", "html", "shtml")

		if (Customer.findAll().length == 0)
			Customer.create.name("Unassigned Users").street("Dummy Street").zip("90402").city("In the Woods!").save

		// Build SiteMap
		import Role._
		import User._
		def sitemap = SiteMap(
			Menu.i("Login") / "index" >> If(() => !User.loggedIn_?, () => RedirectResponse("/home")),
			Menu.i("Home") / "home" >> login_?,

			Menu.i("Customers") / "customers" >> has_?(Administrator, Accounting),
			Menu.param[Customer]("Customer", "Customer", s => CustomerCache.find(s),
				customer => customer.id.is.toString) / "customer" >> Hidden >> has_?(Administrator, Accounting)

			>> User.AddUserMenusAfter >> Hidden
		)

		// Using [[ag.bett.lift.bhtml.NginxSendfileResponse]]
		LiftRules.dispatch.append {
			// File Download
			case Req("pdf" :: "download" :: name :: Nil, _, GetRequest) =>
				() => Full(NginxSendfileResponse("/sendfile/pdf", name, "application/pdf", List(("Content-Disposition" -> "attachment"))))
			// Inline display
			case Req("pdf" :: "display" :: name :: Nil, _, GetRequest) =>
				() => Full(NginxSendfileResponse("/sendfile/pdf", name, "application/pdf", Nil))
		}

		// set the sitemap.	Note if you don't want access control for
		// each page, just comment this line out.
		LiftRules.setSiteMapFunc(() => User.sitemapMutator(sitemap))

		// Use jQuery 1.4
		LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

		//Show the spinny image when an Ajax call starts
		LiftRules.ajaxStart =
			Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

		// Make the spinny image go away when it ends
		LiftRules.ajaxEnd =
			Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

		// Force the request to be UTF-8
		LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

		// What is the function to test if a user is logged in?
		LiftRules.loggedInTest = Full(() => User.loggedIn_?)

		// Make file uploads to be written onto disk instead of ram
		LiftRules.handleMimeFile = OnDiskFileParamHolder.apply

		// Use HTML5 for rendering
		LiftRules.htmlProperties.default.set((r: Req) =>
			new Html5Properties(r.userAgent))


		// Make a transaction span the whole HTTP request
		S.addAround(DB.buildLoanWrapper)
	}
}
