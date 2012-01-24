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
import ag.bett.lift.bootstrap.actors._

import ag.bett.lift.bhtml._

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.json._

import java.sql.{ResultSet, Types}
import java.lang.reflect.Method
import java.util.{Date, Calendar, TimeZone}
import java.math.{MathContext, RoundingMode}


import scala.xml._


trait BootstrappedMapper[A <: KeyedMapper[Long, A]] {
	this: A =>

	private val thisTyped = this.asInstanceOf[A]

	lazy val cssId = scala.util.Random.nextLong
	def cssName(a: String): String = cssName(Full(a))
	def cssName(a: Box[String] = Empty): String = {
		val pri = this.primaryKeyField.is
		val ret = "%s-%s".format(this.dbName.toLowerCase, if (pri == -1) cssId else pri)
		a match {
			case Full(b: String) => ret + "-" + b
			case _ => ret
		}
	}

	def noop: JsCmd = Noop

	def saveJsNoop: JsCmd = {
		this.save; Noop
	}

	def saveJs(sel: String): JsCmd = saveJs(Full(sel))
	def saveJs(sel: Box[String] = Empty): JsCmd =
		this.save match {
			//case true => BHtml.Msg.success(S.?("saved!")) & (sel match {
			case true => sel match {
				case Full(fsel) => BHtml.Fx.validated(fsel) & BHtml.Fx.success(fsel)
				case _ => Noop
			}
			//case false => BHtml.Msg.error(S.?("whoops!")) & (sel match {
			case false => sel match {
				case Full(fsel) => BHtml.Fx.invalidated(fsel) & BHtml.Fx.failed(fsel)
				case _ => Noop
			}
		}

	def deleteJs(sel: String, name: String): JsCmd = deleteJs(sel, Full(name))
	def deleteJs(sel: String, name: Box[String] = Empty, func: JsCmd = Noop): JsCmd = {
		val delCssName = cssName("delete-form")

		val hideJs = BHtml.delay(50, JsRaw("""$('#%s').bind('hidden', function() { $('#%s').remove(); }).modal('hide')""".format(delCssName, delCssName))).cmd
		def delete_!!(sel: Box[String] = Empty): JsCmd =
			this.delete_! match {
				case true => (sel match {
					case Full(fsel) => BHtml.Fx.remove(fsel) & func
					case _ => func
				}) & hideJs
				case false => (sel match {
					case Full(fsel) => BHtml.Fx.failed(fsel)
					case _ => Noop
				}) & hideJs
			}

		val deleteXhtml = <div class="modal hide fade" id={delCssName} style="display:none;"> 
			<div class="modal-header">
				<a href="#" class="close">×</a>
				<h3><lift:loc locid="really.delete.this?">Really delete this {this.getClass().getSimpleName()}?</lift:loc></h3>
			</div>
			<div class="modal-body">
				{name match { case Full(n) => <p>{n}</p> case _ => NodeSeq.Empty }}
				<br/>
				{User.currentUser match { case Full(u) if u.superUser.is => <pre><code>{this.toString}</code></pre> case _ => NodeSeq.Empty}}
			</div>
			<div class="modal-footer">
				<button class="btn danger" onclick={SHtml.ajaxInvoke(() => delete_!!(Full(sel)))._2.toJsCmd}><lift:loc locid="yes">Yes</lift:loc></button>
				<button class="btn" onclick={"javascript: %s".format(hideJs.toJsCmd)}><lift:loc locid="no">No</lift:loc></button>
			</div>
		</div>

		SetHtml("modals", deleteXhtml) &
		JsRaw("""$('#%s').modal({ backdrop:true, keyboard:true }).modal('show')""".format(delCssName))
	}

}


trait CustomerField[A <: Mapper[A]] {
	this: A =>

	private val thisTyped = this.asInstanceOf[A]

	object customer extends MappedLongForeignKey(thisTyped, Customer) with LifecycleCallbacks {
		override def dbColumnName = "customer_id"
		override def dbNotNull_? = true
		override def dbIndexed_? = true
		override def defaultValue = 1L

		override def beforeSave {
			super.beforeSave
			User.currentUser match {
				case Full(u: User) if this.is == 0L =>
					this.set(u.customer.is)
				case _ =>
			}
		}

		override def obj = CustomerCache !! this.is match {
			case Full(a: Customer) => Full(a)
			case _ => Empty
		}

		def buildDisplayList: List[(Long, String)] =
			CustomerCache.all.map(c => (c.id.is, c.displayName))

		def toAjaxForm: NodeSeq = toAjaxForm(Noop)

		def toAjaxForm(js: JsCmd): NodeSeq = SHtml.ajaxSelectObj[Long](buildDisplayList,
			Full(is), (v: Long) => {
				set(v); js
			})
	}

	def customerName = customer.obj match {
		case Full(z: Customer) => z.displayName
		case _ => "(not found)"
	}

}


trait ActivateField[A <: Mapper[A]] {
	this: A =>

	private val thisTyped = this.asInstanceOf[A]

	object active extends MappedBoolean(thisTyped) {
		override def dbNotNull_? = true
		override def dbIndexed_? = true
		override def defaultValue = true
	}

}


trait CreatedByTrait[A <: Mapper[A]] {
	this: A =>

	private val thisTyped = this.asInstanceOf[A]

	object createdBy extends MappedLongForeignKey(thisTyped, User) with LifecycleCallbacks {
		override def dbColumnName = "createdby_id"
		override def dbIndexed_? = true
		override def beforeCreate {
			super.beforeCreate
			User.currentUser match {
				case Full(u: User) => this.set(u.id.is)
				case _ =>
			}
		}
	}

}


trait UpdatedByTrait[A <: Mapper[A]] {
	this: A =>

	private val thisTyped = this.asInstanceOf[A]

	object updatedBy extends MappedLongForeignKey(thisTyped, User) with LifecycleCallbacks {
		override def dbColumnName = "updatedby_id"
		override def dbIndexed_? = true
		override def beforeSave {
			super.beforeSave
			User.currentUser match {
				case Full(u: User) => this.set(u.id.is)
				case _ =>
			}
		}
	}

}


trait Stamping[A <: Mapper[A]]
	extends CreatedUpdated
	with CreatedByTrait[A]
	with UpdatedByTrait[A] {
	this: A =>

}



