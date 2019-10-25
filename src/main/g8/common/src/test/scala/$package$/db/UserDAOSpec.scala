package $package$.db

import $package$.db.UserDAO.DbUser
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, BeforeAndAfter}

import scala.concurrent.Future

class UserDAOSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter{

  //import UserDAO.DbUser

  var dao: UserDAO = _

  before {
    dao = UserDAO("h2mem_dc")
  }

  after {
    dao.db.close()
  }

  val name = "username"
  val email = "email@example.com"

  def addUser(): Future[Long] = {
    dao.addUser(name, email)
  }

  def createAndAddUser(): Future[DbUser] = for {
    _ <- dao.createTables()
    id: Long <- addUser
    u <- dao.getUser(id)
  } yield u.get

  def assertDefaultUser(user: DbUser): Assertion = {
    assert(user._2 == name)
    assert(user._3 == email)
  }

  behavior of "Users"

  it should "create the right tables" in {

    for {
      _ <- dao.createTables()
      names <- dao.tableNames()
    } yield {
      assert(names.size == 1)
      assert(names.count(_.equalsIgnoreCase("users")) == 1)
    }
  }

  it should "create a new user" in {
    for {
      u <- createAndAddUser()
    } yield assertDefaultUser(u)
  }
}
