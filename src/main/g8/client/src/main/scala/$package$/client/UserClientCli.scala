package $package$.client

import java.util.regex.Pattern

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import $package$.grpc.{AddUserRequest, AddUserResponse, GetUserRequest, ListUserRequest, UserResponse, UserServiceClient}
import org.rogach.scallop.exceptions.{Help, ScallopException, ScallopResult, Version}
import org.rogach.scallop.{ScallopConf, Subcommand, throwError}

import scala.concurrent.{ExecutionContext, Future}

object UserClientApp extends App {

    val client = UserClientCli()
    val regex = Pattern.compile("[^\\\s\"']+|\"([^\"]*)\"|'([^']*)'")

    while (true) {
        Console.print("usercli> ")
        val cmd = stringSplitter(scala.io.StdIn.readLine())
        val conf = new CliConf(cmd)
        try {
            conf.verify()
        } catch {
            case e: ScallopException => conf.printHelp()
        }
        if (conf.verified) client.exec(conf)
    }

    def stringSplitter(str: String): Seq[String] = {
        var ret: Seq[String] = Seq()
        val matcher = regex.matcher(str)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                ret = ret :+ matcher.group(1)
            }
            else if (matcher.group(2) != null) {
                ret = ret :+ matcher.group(2)
            }
            else {
                ret = ret :+ matcher.group()
            }
        }
        ret
    }
}

object UserClientCli {

    def apply(): UserClientCli = {
        val system = ActorSystem("userclientcli")
        val materializer = ActorMaterializer.create(system)
        new UserClientCli()(system, materializer)
    }

}

class UserClientCli()(implicit system: ActorSystem, materializer: ActorMaterializer) {

    import scala.concurrent.Await
    import scala.util.{Failure, Success}
    import scala.concurrent.duration._

    implicit val ec: ExecutionContext = system.dispatcher

    val client = UserServiceClient(GrpcClientSettings.fromConfig(clientName = "project.WithHostAndPort"))

    def wait[A](f: Future[A]): A = {
        Await.result(f, 2.seconds)
    }

    def userToStr(ur: UserResponse): String = s"id: \${ur.id}, name: \${ur.name}, email: \${ur.email}"

    def exec(conf: CliConf): Unit = {
        conf.subcommand match {
            case None => conf.printHelp()
            case Some(conf.create) =>
                val name = conf.create.name()
                val email = conf.create.email()
                val resp: AddUserResponse = wait(client.addUser(AddUserRequest(name, email)))
                println(s"id: \${resp.id}, name: \$name, email: \$email")
            case Some(conf.get) =>
                val id = conf.get.id()
                val resp = wait(client.getUser(GetUserRequest(id)))
                resp.user match {
                    case None => println("no user with id: " + id)
                    case Some(userResponse) => println(userToStr(userResponse))
                }
            case Some(conf.list) =>
                val resp = wait(client.listUser(ListUserRequest()))
                println("Total number of users: " + resp.users.size)
                resp.users.foreach(ur => println(userToStr(ur)))
            case Some(conf.exit) =>
                val result = Await.result(system.terminate(), 5.seconds)
                println(result)
                sys.exit(0)
            case Some(_) => conf.printHelp()
        }
    }


}

class CliConf(arguments: Seq[String]) extends ScallopConf(arguments) {

    val create = new Subcommand("add") {
        val name = opt[String](required = true, name = "name", descr = "name of users")
        val email = opt[String](required = true, name = "email", descr = "user's emal")
    }

    val list = new Subcommand("list")

    val get = new Subcommand("get") {
        val id = opt[Long](required = false, name = "id", descr = "user id")
    }

    val exit = new Subcommand("exit")

    addSubcommand(create)
    addSubcommand(list)
    addSubcommand(get)
    addSubcommand(exit)

    //We need to overwrite the default error handler as we dont want to exit the application
    //when there is an error in the input
    errorMessageHandler = (m: String) => {
        verified = false
        println("Could not verify input: " + m)
        printHelp()
    }

    //Override default onError to not exit
    override protected def onError(e: Throwable): Unit = e match {
        case r: ScallopResult if !throwError.value => r match {
            case Help("") =>
                builder.printHelp
            //sys.exit(0)
            case Help(subname) =>
                builder.findSubbuilder(subname).get.printHelp
                verified = false
            //sys.exit(0)
            case Version =>
                builder.vers.foreach(println)
            //sys.exit(0)
            case ScallopException(message) => errorMessageHandler(message)
        }
        case e => {
            Console.println("throwing error")
            throw e
        }
    }

}
