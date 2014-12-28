import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.db.DB
import play.api.test.FakeApplication
import play.api.test.Helpers.{ running, inMemoryDatabase }

import models.Computer

@RunWith(classOf[JUnitRunner])
object ModelSpec extends Specification {
  "Model" title
  
  // -- Helpers
  
  def dateIs(date: java.util.Date, str: String) = 
    new java.text.SimpleDateFormat("yyyy-MM-dd").format(date) == str

  /** Run `block` with connection from a fake application */
  def withFakeConnection[T](block: java.sql.Connection => T): T = {
    val app = FakeApplication(
      additionalConfiguration = inMemoryDatabase())

    running(app) { DB.withConnection(block)(app) }
  }
  
  // --
  
  "Computer model" should {
    "be retrieved by id" in withFakeConnection { implicit con =>
      Computer.findById(21) aka "computer" must beSome.which {
        case macintosh =>
          (macintosh.name aka "name" must_== "Macintosh") and (
            macintosh.introduced aka "introduction date" must beSome.which(
              dateIs(_, "1984-01-24")))

      }
    }
    
    "be listed along its companies" in withFakeConnection { implicit con =>
      Computer.list() aka "computers" must beLike {
        case computers =>
          (computers.total aka "total count" must equalTo(574)) and (
            computers.items aka "items count" must have length(10))

      }
    }
    
    "be updated if needed" in withFakeConnection { implicit con =>
      val computer = Computer(
        name = "The Macintosh", 
        introduced = None, 
        discontinued = None, 
        companyId = Some(1))

      (Computer.update(21, computer) aka "update count" must_== 1) and (
        Computer.findById(21) aka "found computer" must beSome.which {
          case macintosh =>
            (macintosh.name aka "name" must_== "The Macintosh") and (
              macintosh.introduced aka "introduction date" must beNone)
        
        })
    }
  }
}
