package models

import java.util.Date
import java.sql.Connection

import anorm.{ SqlStringInterpolation, ~ }
import anorm.SqlParser.{ date, long, scalar, str }

case class Company(id: Option[Long] = None, name: String)

case class Computer(
  id: Option[Long] = None, 
  name: String, 
  introduced: Option[Date], 
  discontinued: Option[Date], 
  companyId: Option[Long])

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Computer {
  
  // -- Parsers
  
  /**
   * Parse a Computer from a ResultSet
   */
  val simple = {
    long("computer.id").? ~
    str("computer.name") ~
    date("computer.introduced").? ~
    date("computer.discontinued").? ~
    long("computer.company_id").? map {
      case id ~ name ~ introduced ~ discontinued ~ companyId => 
        Computer(id, name, introduced, discontinued, companyId)
    }
  }
  
  /**
   * Parse a (Computer,Company) from a ResultSet
   */
  val withCompany = Computer.simple ~ Company.simple.? map {
    case computer ~ company => (computer, company)
  }
  
  // -- Queries
  
  /**
   * Retrieve a computer from the id.
   */
  def findById(id: Long)(implicit con: Connection): Option[Computer] = 
    SQL"select * from computer where id = $id" as Computer.simple.singleOpt
  
  /**
   * Return a page of (Computer, Company).
   *
   * @param page Page to display
   * @param pageSize Number of computers per page
   * @param orderBy Computer property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%")(implicit con: Connection): Page[(Computer, Option[Company])] = {
    
    val offset = pageSize * page
    
    val computers = SQL"""
        select * from computer 
        left join company on computer.company_id = company.id
        where computer.name like $filter
        order by $orderBy nulls last
        limit $pageSize offset $offset
      """ as Computer.withCompany.*

    val totalRows = SQL"""
        select count(*) from computer 
        left join company on computer.company_id = company.id
        where computer.name like $filter
      """ as scalar[Long].single

    Page(computers, page, offset, totalRows)
  }
  
  /**
   * Update a computer.
   *
   * @param id The computer id
   * @param computer The computer values.
   */
  def update(id: Long, computer: Computer)(implicit con: Connection): Int = 
    SQL"""
        update computer set name = ${computer.name}, 
        introduced = ${computer.introduced}, 
        discontinued = ${computer.discontinued}, 
        company_id = ${computer.companyId} where id = $id
      """.executeUpdate()
  
  /**
   * Insert a new computer.
   *
   * @param computer The computer values.
   */
  def insert(computer: Computer)(implicit con: Connection): Int = 
    SQL"""
      insert into computer values (
        (select next value for computer_seq), ${computer.name}, 
        ${computer.introduced}, ${computer.discontinued}, ${computer.companyId})
      """.executeUpdate()
  
  /**
   * Delete a computer.
   *
   * @param id Id of the computer to delete.
   */
  def delete(id: Long)(implicit con: Connection): Int = 
    SQL"delete from computer where id = $id".executeUpdate()
  
}

object Company {
    
  /**
   * Parse a Company from a ResultSet
   */
  val simple = long("company.id").? ~ str("company.name") map {
    case id ~ name => Company(id, name)
  }
  
  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options(implicit con: Connection): Seq[(String,String)] = 
    SQL"select * from company order by name".as(Company.simple.*).
      foldLeft(Seq[(String, String)]()) { (cs, c) =>
        c.id.fold(cs) { id => cs :+ (id.toString -> c.name) }
      }

}

