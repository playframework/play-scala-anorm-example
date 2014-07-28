package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.DB
import play.api.Play.current

import views._
import models._

/**
 * Manage a database of computers
 */
object Application extends Controller { 
  
  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.list(0, 2, ""))
  
  /**
   * Describe the computer form (used in both edit and create screens).
   */ 
  val computerForm = Form(
    mapping(
      "id" -> ignored(None:Option[Long]),
      "name" -> nonEmptyText,
      "introduced" -> optional(date("yyyy-MM-dd")),
      "discontinued" -> optional(date("yyyy-MM-dd")),
      "company" -> optional(longNumber)
    )(Computer.apply)(Computer.unapply)
  )
  
  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */  
  def index = Action { Home }
  
  /**
   * Display the paginated list of computers.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on computer names
   */
  def list(page: Int, orderBy: Int, filter: String) = Action { implicit req => 
    DB withConnection { implicit con =>
      Ok(html.list(
        Computer.list(page = page, orderBy = orderBy, 
          filter = ("%"+filter+"%")), orderBy, filter))
    }
  }
  
  /**
   * Display the 'edit form' of a existing Computer.
   *
   * @param id Id of the computer to edit
   */
  def edit(id: Long) = Action {
    DB withConnection { implicit con =>
      Computer.findById(id).fold[Result](NotFound) { computer =>
        Ok(html.editForm(id, computerForm.fill(computer), Company.options))
      }
    }
  }
  
  /**
   * Handle the 'edit form' submission 
   *
   * @param id Id of the computer to edit
   */
  def update(id: Long) = Action { implicit request =>
    DB withConnection { implicit con =>
      computerForm.bindFromRequest.fold(formWithErrors =>
        BadRequest(html.editForm(id, formWithErrors, Company.options)),
        computer => {
          Computer.update(id, computer)
          Home.flashing("success" -> 
            s"Computer ${computer.name} has been updated")
        })
    }
  }
  
  /**
   * Display the 'new computer form'.
   */
  def create = Action {
    DB withConnection { implicit con => 
      Ok(html.createForm(computerForm, Company.options))
    }
  }
  
  /**
   * Handle the 'new computer form' submission.
   */
  def save = Action { implicit request =>
    DB withConnection { implicit con =>
      computerForm.bindFromRequest.fold(formWithErrors => 
        BadRequest(html.createForm(formWithErrors, Company.options)),
      computer => {
        Computer.insert(computer)
        Home.flashing("success" -> 
          s"Computer ${computer.name} has been created")
      })
    }
  }
  
  /**
   * Handle computer deletion.
   */
  def delete(id: Long) = Action {
    DB withConnection { implicit con =>
      Computer.delete(id)
      Home.flashing("success" -> "Computer has been deleted")
    }
  }
}
            
