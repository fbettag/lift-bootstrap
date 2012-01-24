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

import ag.bett.lift.bootstrap.lib._
import ag.bett.lift.bootstrap.model._
import ag.bett.lift.bootstrap.actors._

import ag.bett.lift.bhtml._

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._

import scala.xml._
import java.util.{Date, Calendar}


class CustomerName(customer: Customer) {
	def render = "* *" #> "[%s] %s".format(customer.id.is, customer.displayName)
}


class CustomerList {
	def render =
		".customer_row *" #> CustomerCache.all.map(c =>
			".customer_id *" #> <a href={"/customer/%s".format(c.id.is)}>{c.id.is}</a> &
			".customer_name *" #> <a href={"/customer/%s".format(c.id.is)}>{c.displayName}</a>
		)

}


class CustomerAdd extends CustomerForm(Customer.create)
class CustomerForm(customer: Customer) {
	def render =
		".customer_id *" #>					customer.id.toString &
		".customer_displayName *" #>		customer.displayName &
		".customer_name" #>					BHtml.text[Long, Customer](customer.name) &
		".customer_nameadd" #>				BHtml.text[Long, Customer](customer.nameadd) &
		".customer_street" #>				BHtml.text[Long, Customer](customer.street) &
		".customer_zip" #>					BHtml.text[Long, Customer](customer.zip) &
		".customer_city" #>					BHtml.text[Long, Customer](customer.city) &
		".customer_vatid" #>				BHtml.text[Long, Customer](customer.vatid) &
		".customer_country" #>				customer.country.toAjaxForm &
		".customer_company" #>				BHtml.checkbox[Long, Customer](customer.company) &
		".customer_active" #>				BHtml.checkbox[Long, Customer](customer.active) &
		".customer_save [onclick]" #>		BHtml.save[Long, Customer](customer, () => RedirectTo("/customers")) &
		".customer_reset [onclick]" #>		BHtml.reset[Long, Customer](customer)

}

