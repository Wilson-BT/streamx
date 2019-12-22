package com.streamxhub.flink.core.util

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Try
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.{Condition, ReentrantLock}

import org.json4s.DefaultFormats

import scala.collection.JavaConversions._

/**
 *
 * @param instance : 当前连接的实例名称,用户自定义,字符串类型,就是一个连接的名字,用于区分多个连接
 * @param driver   : MySQL driver class (必填)
 * @param url      : MySQL url (必填)
 * @param user     : MySQL user (选填,如不填需要用户在url中设置)
 * @param password : MySQL password(选填,如不填需要用户在url中设置)
 */
case class MySQLConfig(instance: String, driver: String, url: String, user: String, password: String)

object MySQLUtils {


  @transient
  implicit private lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  /**
   * 默认一个实例连接池大小...
   */
  private[this] val initConnSize = 10

  private[this] val waitingTime = 1000

  private val lock: ReentrantLock = new ReentrantLock()

  private val lockMonitor: mutable.Map[String, String] = new ConcurrentHashMap[String, String]()

  private val connectionCondition: mutable.Map[String, Condition] = new ConcurrentHashMap[String, Condition]()

  private val connectionInit: mutable.Map[String, AtomicBoolean] = new ConcurrentHashMap[String, AtomicBoolean]()

  private val collectionPool: mutable.Map[String, util.Queue[Connection]] = new ConcurrentHashMap[String, util.Queue[Connection]]()

  private[this] val collectionHolder = new ThreadLocal[ConcurrentHashMap[String, Connection]]

  /**
   * 将查询的一行数据的所有字段封装到Map里,返回List。。。
   *
   * @param sql
   * @param config
   * @return
   */
  def select(sql: String)(implicit config: MySQLConfig): List[Map[String, _]] = {
    val conn = getConnection(config)
    select(conn, sql)
  }

  def select(conn: Connection, sql: String)(implicit config: MySQLConfig): List[Map[String, _]] = {
    if (sql == null || sql.isEmpty) List.empty else {
      var stmt: Statement = null
      var result: ResultSet = null
      try {
        stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        result = stmt.executeQuery(sql)
        val count = result.getMetaData.getColumnCount
        val array = ArrayBuffer[Map[String, Any]]()
        while (result.next()) {
          var map = Map[String, Any]()
          for (x <- 1 to count) {
            val key = result.getMetaData.getColumnLabel(x)
            val value = result.getObject(x)
            map += key -> value
          }
          array += map
        }
        if (array.isEmpty) List.empty else array.toList
      } catch {
        case ex: Exception => ex.printStackTrace()
          List.empty
      } finally {
        close(config, conn, stmt, result)
      }
    }
  }

  /**
   * 获取一条记录
   *
   * @param sql
   * @return
   */
  def fetch(sql: String)(implicit config: MySQLConfig): Option[Map[String, _]] = select(sql).headOption

  /**
   * 直接查询一个对象
   *
   * @param sql
   * @param config
   * @param manifest
   * @tparam T
   * @return
   */
  def query[T](sql: String)(implicit config: MySQLConfig, manifest: Manifest[T]): List[T] = {
    toObject[T](select(sql))
  }

  def query[T](connection: Connection, sql: String)(implicit config: MySQLConfig, manifest: Manifest[T]): List[T] = {
    toObject[T](select(connection, sql))
  }

  private[this] def toObject[T](list: List[Map[String, _]])(implicit manifest: Manifest[T]): List[T] = if (list.isEmpty) List.empty else list.map(x => JsonUtils.read[T](JsonUtils.write(x)))

  def executeBatch(sql: Iterable[String])(implicit config: MySQLConfig): Int = {
    var conn: Connection = null
    Try(sql.size).getOrElse(0) match {
      case 0 => 0
      case 1 => update(sql.head)
      case _ =>
        conn = getConnection(config)
        conn.setAutoCommit(false)
        val ps = conn.prepareStatement("select 1")
        var index: Int = 0
        var count = 0
        try {
          sql.foreach(x => {
            ps.addBatch(x)
            index += 1
            if (index > 0 && index % 1000 == 0) {
              count += ps.executeBatch().sum
              conn.commit()
              ps.clearBatch()
            }
          })
          count += ps.executeBatch().sum
          conn.commit()
          conn.setAutoCommit(true)
          count
        } catch {
          case ex: Exception => ex.printStackTrace()
            0
        } finally {
          close(config, conn, null, null)
        }
    }
  }

  /**
   *
   * 用于执行 INSERT、UPDATE 或 DELETE 语句以及 SQL DDL（数据定义语言）语句，例如 CREATE TABLE 和 DROP TABLE。
   * INSERT、UPDATE 或 DELETE 语句的效果是修改表中零行或多行中的一列或多列。
   * executeUpdate 的返回值是一个整数（int），指示受影响的行数（即更新计数）。
   * 对于 CREATE TABLE 或 DROP TABLE 等不操作行的语句，executeUpdate 的返回值总为零
   *
   * @param sql
   * @return
   */
  def update(sql: String)(implicit config: MySQLConfig): Int = {
    val conn = getConnection(config)
    var statement: Statement = null
    try {
      statement = conn.createStatement
      statement.executeUpdate(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        -1
    } finally {
      close(config, conn, statement, null)
    }
  }

  /**
   * 查询返回一行记录...
   *
   * @param sql
   * @return
   */
  def selectOne(sql: String)(implicit config: MySQLConfig): Map[String, _] = {
    val conn = getConnection(config)
    var stmt: Statement = null
    var result: ResultSet = null
    try {
      stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      result = stmt.executeQuery(sql)
      val count = result.getMetaData.getColumnCount
      if (!result.next()) Map.empty else {
        var map = Map[String, Any]()
        for (x <- 1 to count) {
          val key = result.getMetaData.getColumnLabel(x)
          val value = result.getObject(x).asInstanceOf[Any]
          map += key -> value
        }
        map
      }
    } catch {
      case ex: Exception => ex.printStackTrace()
        Map.empty
    } finally {
      close(config, conn, stmt, result)
    }
  }

  /**
   *
   * 方法execute：
   * 可用于执行任何SQL语句，返回一个boolean值，表明执行该SQL语句是否返回了ResultSet。
   * 如果执行后第一个结果是ResultSet，则返回true，否则返回false。
   * 但它执行SQL语句时比较麻烦，通常我们没有必要使用execute方法来执行SQL语句，而是使用executeQuery或executeUpdate更适合。
   * 但如果在不清楚SQL语句的类型时则只能使用execute方法来执行该SQL语句了
   *
   * @param sql
   * @return
   */
  def execute(sql: String)(implicit config: MySQLConfig): Boolean = {
    val conn = getConnection(config)
    var stmt: Statement = null
    try {
      stmt = conn.createStatement
      stmt.execute(sql)
    } catch {
      case ex: Exception => ex.printStackTrace()
        false
    } finally {
      close(config, conn, null, null)
    }
  }

  def getConnection(config: MySQLConfig): Connection = {
    lock.lock()
    val prefix = s"${config.instance}"
    val condition = connectionCondition.getOrElseUpdate(prefix, lock.newCondition())

    def needWait() = lockMonitor.getOrElse(prefix, null) == prefix

    try {

      while (needWait()) {
        condition.await()
      }
      //加入当前实例为监控对象
      lockMonitor += prefix -> prefix

      connectionInit.getOrElseUpdate(prefix, new AtomicBoolean(false))
      val conn = Try(collectionHolder.get()(prefix)).getOrElse(null)
      if (conn != null) conn else {
        val size = Try(collectionPool(prefix).size).getOrElse(0)

        /**
         * 如果10个连接全部被占用,而且长时间不释放,则等待,等待释放在返回连接
         */
        if (size == 0) {
          val initStatus = connectionInit(prefix)
          //该实例的连接池已经初始化过,10个连接已经全部放出去了,则等待收回....
          if (initStatus.get()) {
            println("getConnection waiting....... ")
            Thread.sleep(waitingTime)
            return getConnection(config)
          }

          for (_ <- 0 to initConnSize) {
            val conn = config.user match {
              case null => DriverManager.getConnection(config.url)
              case _ => DriverManager.getConnection(config.url, config.user, config.password)
            }
            //往连接池里放连接....
            collectionPool.getOrElseUpdate(prefix, new util.LinkedList[Connection]).add(conn)
          }
          initStatus.set(true)
        }

        val map = new ConcurrentHashMap[String, Connection]
        val conn = collectionPool(prefix).poll()
        map += prefix -> conn
        collectionHolder.set(map)
        //移除当前的监控对象...
        if (needWait()) {
          condition.signalAll()
        }
        lockMonitor -= prefix
        conn
      }
    } finally {
      lock.unlock()
    }
  }

  def close(config: MySQLConfig, connection: Connection, statement: Statement, resultSet: ResultSet) = {
    if (resultSet != null) {
      resultSet.close()
    }
    if (statement != null) {
      statement.close()
    }
    if (connection != null) {
      //此连接关闭会将资源回收到连接池里....
      Try(collectionHolder.get().remove(config.instance))
      collectionPool.getOrElseUpdate(config.instance, new util.LinkedList[Connection]).add(connection)
    }
  }


}