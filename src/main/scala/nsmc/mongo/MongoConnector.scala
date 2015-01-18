package nsmc.mongo

import com.mongodb.ServerAddress
import com.mongodb.casbah.Imports._

private[nsmc] class MongoConnector(databaseName: String, collectionName: String, interval: MongoInterval) extends nsmc.Logging {

  logDebug(s"Obtaining interval data for collection '$collectionName' in database '$databaseName' at '${interval.destination.host}:${interval.destination.port}'")
  private val server = new ServerAddress(interval.destination.host, interval.destination.port)
  private val conf = interval.destination.conf
  private val mongoClient = if (conf.user.isDefined && conf.password.isDefined) {
    val credentials = MongoCredential.createMongoCRCredential(conf.user.get, databaseName, conf.password.get.toCharArray)
    logDebug(s"Connecting with password for user '${conf.user.get}'")
    MongoClient(server, List(credentials))
  } else {
    logDebug(s"Connecting without credentials")
    MongoClient(server)
  }

  // fetch data from the given interval
  def getData: Iterator[DBObject] = {

    val db = mongoClient.getDB(databaseName)
    val col = db(collectionName)

    val cursor = col.find()
    val withMin = if (interval.min == null || interval.min.values.size == 0) cursor else cursor.addSpecial("$min", interval.min)
    val withMax = if (interval.max == null || interval.max.values.size == 0) withMin else cursor.addSpecial("$max", interval.max)
    withMax.toIterator
  }

  def close() : Unit = {
    mongoClient.close()
  }

}