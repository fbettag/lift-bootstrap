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

package ag.bett.lift.bootstrap.lib

import ag.bett.lift.bootstrap.model._

import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd

import java.util.{Date, Calendar, TimeZone}
import org.joda.time._
import org.joda.time.format._


object DateTimeHelpers {

	var timezone = DateTimeZone.forTimeZone(S.timeZone)

	def getTZ(tz: String): DateTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone(tz))

	def getUserTZ: DateTimeZone = User.currentUser match {
		case Full(u: User) => getTZ(u.timezone.is)
		case _ => timezone
	}

	def getDate: DateTime = new DateTime(getUserTZ)

	def getDate(date: Calendar): DateTime = new DateTime(date, getUserTZ)

	def getDate(date: Date): DateTime = new DateTime(date, getUserTZ)

	def durationAsString(start: DateTime) = (new PeriodFormatterBuilder)
		.printZeroAlways
		.minimumPrintedDigits(2)
		.appendHours
		.appendSuffix(":")
		.appendMinutes
		.appendSuffix(":")
		.appendMinutes
		.toFormatter
		.print(new Period(start, DateTimeHelpers.getDate))

}


