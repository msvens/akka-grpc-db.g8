package $package$.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import $package$.db.UserDAO
import $package$.grpc.{AddUserRequest, GetUserRequest, GetUserResponse, ListUserRequest, UserResponse}
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UserServiceImplSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {

  var dao: UserDAO = _

  val system: ActorSystem = ActorSystem("UserServiceSystem")
  val mat: ActorMaterializer = ActorMaterializer.create(system)

  var service: UserServiceImpl = _

  before {
    dao = UserDAO("h2mem_server_dc")
    dao.createTablesSynced()
    service = new UserServiceImpl(dao)(mat)
  }

  after {
    dao.db.close()
  }

  override def afterAll(): Unit = {
    dao.db.close()
    Await.ready(system.terminate(), 5.seconds)
  }

  def newUser(name: String = "name"): AddUserRequest = {
    AddUserRequest(name, "name@email.com")
  }

  def assertDefaultUser(resp: GetUserResponse): Assertion = {
    assert(resp.user.isDefined)
    assert(resp.getUser.name == "name")
    assert(resp.getUser.email == "name@email.com")
  }


  behavior of "UserServiceImpl"

  it should "create a user" in {
    service.addUser(newUser()).map(resp => assert(resp.id > 0))
  }

  it should "get a user" in {
    for {
      resp <- service.addUser(newUser())
      user <- service.getUser(GetUserRequest(resp.id))
    } yield assertDefaultUser(user)
  }

  it should "list users" in {
    for {
      id1 <- service.addUser(newUser("user1"))
      id2 <- service.addUser(newUser("user2"))
      users <- service.listUser(ListUserRequest())
    } yield {
      assert(users.users.size == 2)
    }
  }
}
