package $package$.db

import $package$.db.UserDAO.DbUser
import $package$.grpc.{AddUserRequest, ListUserResponse, UserResponse}
import com.softwaremill.id.DefaultIdGenerator
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object UserDAO {

  type DbEmail = String
  type DbName = String
  type DbUser = (Long, DbName, DbEmail)

  def apply(dbConfig: String)(implicit ec: ExecutionContext): UserDAO = {
    new UserDAO(DatabaseConfig.forConfig[JdbcProfile](dbConfig))
  }
}

class UserDAO(protected val config: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext) {

  import config.profile.api._

  val db = config.db

  val userIdGenerator = new DefaultIdGenerator(1, 1)

  class UserTable(tag: Tag) extends Table[DbUser](tag, "Users"){
    def id = column[Long]("id", O.PrimaryKey)
    def name = column[String]("name")
    def email = column[String]("email")
    def * = (id, name, email)
  }

  private val users = TableQuery[UserTable]

  def createTables(): Future[Unit] = {
    db.run(users.schema.create)
  }

  def createTablesSynced(): Unit = {
    Await.result(createTables(), 1.second)
  }

  def dropTables(): Future[Unit] = {
    db.run(users.schema.dropIfExists)
  }

  def dropTablesSynced(): Unit = {
    Await.result(dropTables(), 1.second)
  }

  def tableNames(): Future[Seq[String]] = for {
    tables <- db.run(MTable.getTables)
  } yield tables.map(t => t.name.name)

  def usersSize(): Future[Int] = {
    db.run(users.size.result)
  }


  def addUser(name: String, email: String): Future[Long] = {
    val id = userIdGenerator.nextId()
    db.run(users += (id, name, email)).map(_ => id)
    //db.run((users returning users.map(_.id)) += ((id, name, email))).map(_ => id)
  }

  def addUser(addUserRequest: AddUserRequest): Future[Long] = {
    assert(addUserRequest.name != "")
    assert(addUserRequest.email != "")
    addUser(addUserRequest.name, addUserRequest.email)
  }

  def getUser(id: Long): Future[Option[DbUser]] = {
    val q = users.filter(_.id === id)
    db.run(q.result.headOption)
  }

  def getGrpcUser(id: Long): Future[Option[UserResponse]] = {
    getUser(id).map{
      case Some(x) => Some(UserResponse(x._1, x._2, x._3))
      case None => None
    }
  }

  def listUsers(): Future[Seq[DbUser]] = {
    val q = for(u <- users) yield u
    db.run(q.result)
  }

  def listGrpcUsers(): Future[ListUserResponse] = {
    for {
      users <- listUsers()
      users1: Seq[UserResponse] = users.map(u => {UserResponse(u._1, u._2, u._3)})
    } yield ListUserResponse(users1)
  }
}