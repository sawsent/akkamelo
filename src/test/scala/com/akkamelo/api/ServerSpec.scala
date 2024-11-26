import ServerSpec.{MockedGet, MockedTransaction, mockedResponse2ResponseDTO, resetProbeAndServer}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.KeepRunning
import akka.testkit.TestProbe
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.domain.state.{Client, TransactionType}
import com.akkamelo.api.adapter.endpoint.ActorResponse2ResponseDTO
import com.akkamelo.api.endpoint.Server
import com.akkamelo.api.endpoint.dto.{ClientGetStatementRequestDTO, TransactionRequestDTO}
import com.akkamelo.api.endpoint.marshalling.CustomMarshalling._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class ServerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec = system.dispatcher

  "POST /clientes/{clientId}/transacoes" should "reply with 200 ok and the BalanceAndLimitJson" in {
    val clientId = 1
    val transactionMock = MockedTransaction(clientId)

    val (testProbe, server) = resetProbeAndServer(9090)
    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case cmd if cmd == transactionMock.actorCommand => sender ! transactionMock.response_OK
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, transactionMock.requestJSON) ~> server.route ~> check {
      testProbe.expectMsg(transactionMock.actorCommand)
      status shouldBe StatusCodes.OK
      responseAs[String] should include (mockedResponse2ResponseDTO(transactionMock.response_OK).payload.toJson.toString())
    }
  }

  it should "return a 422 Unprocessable entity when the Actor responds with ClientActorUnprocessableEntity" in {
    val (testProbe, server) = resetProbeAndServer(9091)

    val clientId = 1
    val transactionMock = MockedTransaction(clientId)

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case cmd if cmd == transactionMock.actorCommand => sender ! transactionMock.response_UNPROCESSABLE_ENTITY
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, transactionMock.requestJSON) ~> server.route ~> check {
      testProbe.expectMsg(transactionMock.actorCommand)
      status shouldBe StatusCodes.UnprocessableEntity
    }
  }

  it should "return 404 if client doesn't exist" in {
    val (testProbe, server) = resetProbeAndServer(9093)

    val clientId = 1
    val transactionMock = MockedTransaction(clientId)

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case cmd if cmd == transactionMock.actorCommand =>
          sender ! transactionMock.response_NOT_FOUND
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, transactionMock.requestJSON) ~> server.route ~> check {
      testProbe.expectMsg(transactionMock.actorCommand)
      status shouldBe StatusCodes.NotFound
    }
  }

  "GET /clientes/{clientId}/extrato" should "return a client statement successfully" in {
    val clientId = 1
    val getMock = MockedGet(clientId)

    val (testProbe, server) = resetProbeAndServer(9094)

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case cmd if cmd == getMock.actorCommand => sender ! getMock.response_OK
      }
      KeepRunning
    })

    Get(s"/clientes/$clientId/extrato") ~> server.route ~> check {
      testProbe.expectMsg(ClientGetStatementCommand(clientId))
      status shouldBe StatusCodes.OK
      responseAs[String] should include (mockedResponse2ResponseDTO(getMock.response_OK).payload.toJson.toString())
    }
  }

  it should "return 404 Not Found if client doesn't exist" in {
    val (testProbe, server) = resetProbeAndServer(9095)

    val clientId = 1
    val getMock = MockedGet(clientId)

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case cmd if cmd == getMock.actorCommand => sender ! getMock.response_NOT_FOUND
      }
      KeepRunning
    })

    Get(s"/clientes/$clientId/extrato") ~> server.route ~> check {
      testProbe.expectMsg(getMock.actorCommand)
      status shouldBe StatusCodes.NotFound
    }
  }
}

object ServerSpec {
  val UNIVERSAL_TIME_STRING: String = "2021-01-01T00:00:00Z"
  val UNIVERSAL_TIME_TIME: LocalDateTime = ZonedDateTime.parse(UNIVERSAL_TIME_STRING, DateTimeFormatter.ISO_DATE_TIME)
    .withZoneSameInstant(ZoneOffset.UTC)
    .toLocalDateTime

  val mockedResponse2ResponseDTO = (response: ClientActorResponse) => ActorResponse2ResponseDTO.toResponseDTO(response)

  def resetProbeAndServer(port: Int)(implicit system: ActorSystem, ec: ExecutionContext, timeout: Timeout): (TestProbe, Server) = {
    val actorSupervisorProbe = TestProbe()
    val server = Server.newStartedAt("localhost", port, actorSupervisorProbe.ref)
    (actorSupervisorProbe, server)
  }



  case class MockedGet(clientId: Int) {
    val requestDTO = ClientGetStatementRequestDTO
    val actorCommand = ClientGetStatementCommand(clientId)
    val response_OK: ClientStatementResponse = ClientStatementResponse(Client.initialWithId(clientId).getStatement.copy(
      balanceInformation = Client.initialWithId(clientId).getStatement.balanceInformation.copy(timestamp = UNIVERSAL_TIME_TIME)
    ))
    val response_NOT_FOUND = ClientDoesntExist
    val response_UNPROCESSABLE_ENTITY = ClientActorUnprocessableEntity
  }

  case class MockedTransaction(clientId: Int, transactionType: TransactionType = TransactionType.CREDIT,
                               value: Int = 100, description: String = "desc") {
    val transactionTypeString = transactionType match {
      case TransactionType.CREDIT => "c"
      case TransactionType.DEBIT => "d"
      case TransactionType.NO_TYPE => "n"
    }

    val requestDTO = TransactionRequestDTO(value, transactionTypeString, description)
    val requestJSON = requestDTO.toJson.toString()
    val actorCommand = ClientAddTransactionCommand(clientId, value, transactionType, description)
    val response_NOT_FOUND = ClientDoesntExist
    val response_UNPROCESSABLE_ENTITY = ClientActorUnprocessableEntity
    val response_OK = ClientBalanceAndLimitResponse(value, 0)
  }
}
