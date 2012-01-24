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
import ag.bett.lift.bhtml._

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

import scala.xml._

object Role extends Enumeration {
	// Staff - Internal
	val Administrator, Accounting, Support = Value

	val Referrer, Customer = Value
}


class User
	extends MegaProtoUser[User]
	with CreatedUpdated
	with CustomerField[User]
	with ActivateField[User]
	with BootstrappedMapper[User] {
	def getSingleton = User

	object roles extends MappedEnumList(this, Role)

	def role_?(ro: Role.Value*) =
		ro.exists(roles.is.contains)

}


object User extends User
with MetaMegaProtoUser[User] {

	override def dbTableName = "users"

	override val basePath: List[String] = "users" :: Nil

	override def fieldOrder = List(id, firstName, lastName, email, locale, timezone, password, customer)

	override def skipEmailValidation = true

	def login(login: String, pass: String): Boolean = try {
		getSingleton.find(By(getSingleton.email, login)) match {
			//case Full(u: User) if u.password.match_?(pass) && u.validated.is && u.active.is && u.customer.obj.open_!.active.is =>
			case Full(u: User) if u.password.match_?(pass) && u.validated.is && u.active.is =>
				getSingleton.logUserIn(u)
				true
			case _ => false
		}
	} catch {
		case _ => false
	}

	def can_?(roles: Role.Value*) =
		User.currentUser match {
			case Full(u: User) if roles.exists(u.roles.is.contains) => true
			case Full(u: User) => false
			case _ => false
		}

	def has_?(roles: Role.Value*) =
		net.liftweb.sitemap.Loc.If(() => can_?(roles: _*), () => {
			val uri = S.uriAndQueryString
			RedirectWithState("/", RedirectState(() => loginRedirect(uri)))
		})

	def login_? =
		net.liftweb.sitemap.Loc.If(() => loggedIn_?, () => {
			val uri = S.uriAndQueryString
			RedirectWithState("/", RedirectState(() => loginRedirect(uri)))
		})

	def isPrivileged_? = can_?(Role.Administrator, Role.Accounting, Role.Support)

	override protected def globalUserLocParams = List(Hidden)

	override def loginMenuLoc: Box[Menu] = Empty
	//override def logoutMenuLoc: Box[Menu] = Empty
	//override def createUserMenuLoc: Box[Menu] = Empty
	//override def lostPasswordMenuLoc: Box[Menu] = Empty
	//override def resetPasswordMenuLoc: Box[Menu] = Empty
	//override def editUserMenuLoc: Box[Menu] = Empty
	//override def changePasswordMenuLoc: Box[Menu] = Empty
	//override def validateUserMenuLoc: Box[Menu] = Empty

	override protected def localForm(user: TheUserType, ignorePassword: Boolean, fields: List[FieldPointerType]): NodeSeq = {
		for {
			pointer <- fields
			field <- computeFieldFromPointer(user, pointer).toList
			if field.show_? && (!ignorePassword || !pointer.isPasswordField_?)
			form <- field.toForm.toList
		} yield <div class="clearfix">
			<label>{field.displayName}</label>
			<div class="input">{form}</div>
		</div>
	}

	override def standardSubmitButton(name: String,  func: () => Any = () => {}) = {
		SHtml.submit(name, func, "class" -> "btn success")
	}

	override def screenWrap = Full(
		<lift:surround with="bootstrap" at="lift-content">
			<div class="container-fluid">
				<div class="content">
					<div class="row">
						<lift:bind />
					</div>
				</div>
			</div>
		</lift:surround>)


	override def lostPasswordXhtml = <div class="hero-unit span14">
		<h1>{S.??("enter.email")}</h1><br/><br/>
		<form method="post" action={S.uri}>
			<fieldset>
				<div class="clearfix">
					<label>{userNameFieldString}</label>
					<div class="input"><user:email /></div>
				</div>
				<div class="actions"><user:submit /></div>
			</fieldset>
		</form>
	</div>

	override def editXhtml(user: TheUserType) = <div class="hero-unit span14">
		<h1><lift:loc locid="edit.your.account">Edit your Account</lift:loc></h1>
		<form method="post" action={S.uri}>
			<fieldset>
				{localForm(user, true, editFields)}
				<div class="actions"><user:submit /></div>
			</fieldset>
		</form>
	</div>

	override def changePasswordXhtml = <div class="hero-unit span14">
		<h1><lift:loc locid="change.password">Change your Password</lift:loc></h1>
		<form method="post" action={S.uri}>
			<fieldset>
				<div class="clearfix">
					<label>{S.??("old.password")}</label>
					<div class="input"><user:old_pwd /></div>
				</div>

				<div class="clearfix">
					<label>{S.??("new.password")}</label>
					<div class="input"><user:new_pwd /></div>
				</div>

				<div class="clearfix">
					<label>{S.??("repeat.password")}</label>
					<div class="input"><user:new_pwd /></div>
				</div>

				<div class="actions"><user:submit /></div>
			</fieldset>
		</form>
	</div>

	override def signupXhtml(user: TheUserType) = <div class="hero-unit span14">
		<h1><lift:loc locid="sign.up">Sign Up</lift:loc></h1>
		<form method="post" action={S.uri}>
			<fieldset>
				{localForm(user, false, signupFields)}
				<div class="actions"><user:submit /></div>
			</fieldset>
		</form>
	</div>

}


