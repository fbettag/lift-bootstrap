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

package ag.bett.lift.bootstrap.model

import ag.bett.lift.bootstrap.lib._
import ag.bett.lift.bootstrap.actors._
import ag.bett.lift.bootstrap.comet._

import ag.bett.lift.bhtml._

import net.liftweb.mapper._
import net.liftweb.http._
import net.liftweb.common._
import net.liftweb.util.Helpers._


class Customer
	extends LongKeyedMapper[Customer]
	with IdPK
	with Stamping[Customer]
	with ActivateField[Customer]
	with BootstrappedMapper[Customer] {
	def getSingleton = Customer

	override def save() = {
		val ret = super.save
		if (ret) CustomerCache ! this
		ret
	}

	object name extends MappedString(this, 120) {
		override def dbNotNull_? = true
		override def dbIndexed_? = true
		override def setFilter = trim _ :: super.setFilter
		override def validations =
			valUnique(S.??("name.taken")) _ ::
			valMinLen(5, S.??("name.too.short")) _ ::
			valMaxLen(120, S.??("name.too.long")) _ ::
			super.validations
	}

	object nameadd extends MappedString(this, 120) {
		override def dbIndexed_? = true
		override def setFilter = trim _ :: super.setFilter
	}

	object street extends MappedString(this, 120) {
		override def dbNotNull_? = true
		override def setFilter = trim _ :: super.setFilter
		override def validations =
			valMinLen(5, S.??("street.too.short")) _ ::
			valMaxLen(120, S.??("street.too.long")) _ ::
			super.validations
	}

	object country extends MappedCountry(this) {
		override def dbNotNull_? = true
		override def defaultValue = Countries.Germany
		def toAjaxForm = SHtml.ajaxSelectObj[Int](buildDisplayList,
			Full(toInt), (v: Int) => {
				this.set(fromInt(v)); net.liftweb.http.js.JsCmds.Noop
			})
	}

	object zip extends MappedString(this, 50) {
		override def dbNotNull_? = true
		override def setFilter = trim _ :: super.setFilter
		override def validations =
			valMinLen(4, S.??("zip.too.short")) _ ::
				valMaxLen(50, S.??("zip.too.long")) _ ::
				super.validations
	}

	object city extends MappedString(this, 120) {
		override def dbNotNull_? = true
		override def setFilter = trim _ :: super.setFilter
		override def validations =
			valMinLen(2, S.??("city.too.short")) _ ::
				valMaxLen(120, S.??("city.too.long")) _ ::
				super.validations
	}

	object company extends MappedBoolean(this) {
		override def defaultValue = true
	}

	object vatid extends MappedString(this, 30) {
		override def setFilter = trim _ :: toUpper _ :: super.setFilter
	}

	def displayName = name.is + " " + nameadd.is

}


object Customer extends Customer
with LongKeyedMetaMapper[Customer] {

	override def dbTableName = "customers"
	//override def dbIndexes = List(Index(IndexField(name)))
	override def fieldOrder = List(name, nameadd, street, zip, city, country, company)

}


