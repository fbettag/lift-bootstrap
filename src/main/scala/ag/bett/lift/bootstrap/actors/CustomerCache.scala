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

package ag.bett.lift.bootstrap.actors

import ag.bett.lift.bootstrap.model._

import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.mapper._

import scala.collection.mutable.HashMap


case object CustomerAll

object CustomerCache extends LiftActor with Loggable {

	protected val customers = new HashMap[Long, Customer]()

	protected def messageHandler = {
		case a: Customer => update(a)
		case a: Long => reply(customers.get(a) match {
			case Some(c: Customer) => c
			case _ => null
		})
		case CustomerAll => reply(sort(customers))
		case _ =>
	}

	protected def update(customer: Customer) {
		logger.debug("Storing Customer: %s".format(customer))
		customers.put(customer.id.is, customer)
	}

	val init = {
		Customer.findAll(OrderBy(Customer.id, Ascending)).foreach(update)

		logger.info("Successfully loaded!")
		true
	}

	protected def sort(hm: HashMap[Long, Customer]) = {
		hm.toList.sortWith((e1, e2) => (e1._1 compareTo e2._1) < 0).map(_._2)
	}

	def find(s: String): Box[Customer] =
		try {
			find(s.toLong)
		} catch {
			case _ => Empty
		}

	def find(i: Long): Box[Customer] =
		this !! i match {
			case Full(a: Customer) => Full(a)
			case _ => Empty
		}

	def all: List[Customer] = {
		val user = User.currentUser match {
			case Full(u: User) => u
			case _ => return List()
		}

		if (user.role_?(Role.Administrator, Role.Support)) {
			return (this !! CustomerAll match {
				case Full(a: List[Customer]) => a
				case _ => List()
			})
		}

		List()
	}

}
