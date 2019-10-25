package $package$.service

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import $package$.db.UserDAO
import $package$.grpc.{AddUserRequest, GetUserRequest, GetUserResponse, UserServiceClient}
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UserServerSpec extends AsyncFlatSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {

  val server = UserServer("h2mem_server_dc")
  val dao = server.dao

  //Client
  implicit val clientSystem = ActorSystem("clientSystem")
  implicit val mat = ActorMaterializer()
  val client = {
    implicit val ec = clientSystem.dispatcher
    val clientSettings = GrpcClientSettings.connectToServiceAt("localhost", 9090).withTls(false)
    UserServiceClient(clientSettings)
  }

  before {
    dao.createTablesSynced()
  }

  after {
    Await.ready(dao.dropTables(), 5.seconds)
  }

  override def afterAll(): Unit = {
    Await.ready(clientSystem.terminate(), 5.seconds)
    server.shutdown(true)
  }

  def newUser(name: String = "name"): AddUserRequest = {
    AddUserRequest(name, "name@email.com")
  }

  def assertDefaultUser(resp: GetUserResponse): Assertion = {
    assert(resp.user.isDefined)
    assert(resp.getUser.name == "name")
    assert(resp.getUser.email == "name@email.com")
  }

  behavior of "UserServer"

  it should "create a new user" in {
    client.addUser(newUser()).map(resp => {assert(resp.id > 0)})
  }

  it should "get a user" in {
    for {
      id <- client.addUser(newUser())
      resp <- client.getUser(GetUserRequest(id.id))
    } yield assertDefaultUser(resp)
  }
}
