package $package$.service

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.{Http2, HttpConnectionContext}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, Materializer}
import $package$.db.UserDAO
import $package$.grpc._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object UserServerApp extends App {
  val server = UserServer()
  println("server started!")
  StdIn.readLine()
  server.shutdown(true)
}

object UserServer {

  def apply(dbConfig: String = "h2mem_dc"): UserServer = {
    val system = ActorSystem("UserServerSystem")
    val materializer = ActorMaterializer.create(system)
    val db = UserDAO(dbConfig)(system.dispatcher)
    new UserServer(db)(system,materializer)
  }

  def withSystem(dbConfig: String = "h2mem_dc")(implicit system: ActorSystem, materializer: ActorMaterializer): UserServer = {
    withDb(UserDAO(dbConfig)(system.dispatcher))
  }

  def withDb(dao: UserDAO)(implicit system: ActorSystem, materializer: ActorMaterializer): UserServer = {
    new UserServer(dao)
  }

}

class UserServer(val dao: UserDAO)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends Config {

  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)

  val service: HttpRequest => Future[HttpResponse] =
    UserServiceHandler(new UserServiceImpl(dao))

  val binding = Http2().bindAndHandleAsync(
    service,
    interface = httpHost,
    port = httpPort,
    connectionContext = HttpConnectionContext())

  // report successful binding
  binding.foreach { binding =>
    println(s"gRPC server bound to: \${binding.localAddress}")
  }

  def shutdown(closeDb: Boolean = false): Future[Terminated] ={
    import scala.concurrent.duration._

    if(closeDb) dao.db.close()

    Await.result(binding, 10.seconds)
      .terminate(hardDeadline = 3.seconds).flatMap(_ => {
      //dbService.dataSource.close
      actorSystem.terminate()
    })
  }


}

class UserServiceImpl(dao: UserDAO)(implicit mat: Materializer) extends UserService {

  import mat.executionContext

  override def addUser(in: AddUserRequest): Future[AddUserResponse] = for {
    l <- dao.addUser(in)
  } yield AddUserResponse(l)

  override def getUser(in: GetUserRequest): Future[GetUserResponse] = {
    dao.getGrpcUser(in.id).map{
      case Some(r) => GetUserResponse(Some(r))
      case None => GetUserResponse()
    }
  }

  override def listUser(in: ListUserRequest): Future[ListUserResponse] = {
    dao.listGrpcUsers()
  }
}
