import ServerSpec.{MockedTransaction, mockedBalanceAndLimitResponse, mockedGetResponse, resetProbeAndServer}
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
import com.akkamelo.api.endpoint.dto.TransactionRequestDTO
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

  "POST /clientes/{clientId}/transacoes" should "process a transaction request successfully" in {
    val clientId = 1
    val transactionRequest = MockedTransaction.requestDTO
    val transactionRequestJson = transactionRequest.toJson.toString()
    val responseDTO = ActorResponse2ResponseDTO.toResponseDTO(mockedBalanceAndLimitResponse)

    val (testProbe, server) = resetProbeAndServer(9090)
    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case ApplyCommand(id, clientAddTransactionCommand: ClientAddTransactionCommand) if id == clientId && clientAddTransactionCommand == MockedTransaction.actorCommand =>
          sender ! mockedBalanceAndLimitResponse
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, transactionRequestJson) ~> server.route ~> check {
      testProbe.expectMsg(ApplyCommand(clientId = clientId, MockedTransaction.actorCommand))
      status shouldBe StatusCodes.OK
      responseAs[String] should include (responseDTO.payload.toJson.toString())
    }
  }

  it should "return a 422 Unprocessable entity when the transaction request is invalid" in {
    val (testProbe, server) = resetProbeAndServer(9091)

    val clientId = 1
    val invalidTransactionRequest = TransactionRequestDTO(-100, "c", "desc")
    val invalidTransactionCommandEuivalent = ClientAddTransactionCommand(1, -100, TransactionType.CREDIT, "desc")
    val invalidTransactionRequestJson = invalidTransactionRequest.toJson.toString()

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case ApplyCommand(id, clientAddTransactionCommand: ClientAddTransactionCommand) if id == clientId && clientAddTransactionCommand == invalidTransactionCommandEuivalent =>
          sender ! ClientActorUnprocessableEntity
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, invalidTransactionRequestJson) ~> server.route ~> check {
      testProbe.expectMsg(ApplyCommand(clientId, invalidTransactionCommandEuivalent))
      status shouldBe StatusCodes.UnprocessableEntity
    }
  }

  it should "return a 422 Unprocessable entity when the transaction type in request is invalid" in {
    val (testProbe, server) = resetProbeAndServer(9092)

    val clientId = 1
    val invalidTransactionRequest = TransactionRequestDTO(-100, "cas", "desc")
    val invalidTransactionRequestJson = invalidTransactionRequest.toJson.toString()

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case ApplyCommand(id, clientAddTransactionCommand: ClientAddTransactionCommand) if id == clientId =>
          sender ! ClientActorUnprocessableEntity
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, invalidTransactionRequestJson) ~> server.route ~> check {
      status shouldBe StatusCodes.UnprocessableEntity
    }
  }

  it should "return 404 if client doesn't exist" in {
    val (testProbe, server) = resetProbeAndServer(9093)

    val clientId = 1
    val transactionRequest = MockedTransaction.requestDTO
    val transactionCommand = MockedTransaction.actorCommand
    val transactionJsonString = transactionRequest.toJson.toString()

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case ApplyCommand(id, _) if id == clientId =>
          sender ! ClientDoesntExist
      }
      KeepRunning
    })

    Post(s"/clientes/$clientId/transacoes").withEntity(ContentTypes.`application/json`, transactionJsonString) ~> server.route ~> check {
      testProbe.expectMsg(ApplyCommand(clientId, transactionCommand))
      status shouldBe StatusCodes.NotFound
    }
  }

  "GET /clientes/{clientId}/extrato" should "return a client statement successfully" in {
    val clientId = 1
    val responseDTO = ActorResponse2ResponseDTO.toResponseDTO(mockedGetResponse)
    val responsePayloadJsonString = responseDTO.payload.toJson.toString()

    val (testProbe, server) = resetProbeAndServer(9094)

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case ApplyCommand(id, _) if id == clientId =>
          sender ! mockedGetResponse
      }
      KeepRunning
    })

    Get(s"/clientes/$clientId/extrato") ~> server.route ~> check {
      testProbe.expectMsg(ApplyCommand(clientId, ClientGetStatementCommand(clientId)))
      status shouldBe StatusCodes.OK
      responseAs[String] should include (responsePayloadJsonString)
    }
  }

  it should "return 404 Not Found if client doesn't exist" in {
    val (testProbe, server) = resetProbeAndServer(9095)

    val clientId = 1
    val transactionRequest = MockedTransaction.requestDTO
    val transactionCommand = MockedTransaction.actorCommand

    val transactionJsonString = transactionRequest.toJson.toString()

    testProbe.setAutoPilot((sender: ActorRef, msg: Any) => {
      msg match {
        case ApplyCommand(id, _) if id == clientId =>
          sender ! ClientDoesntExist
      }
      KeepRunning
    })

    Get(s"/clientes/$clientId/extrato") ~> server.route ~> check {
      testProbe.expectMsg(ApplyCommand(clientId, ClientGetStatementCommand(clientId)))
      status shouldBe StatusCodes.NotFound
    }
  }
}

object ServerSpec {
  val UNIVERSAL_TIME_STRING: String = "2021-01-01T00:00:00Z"
  val UNIVERSAL_TIME_TIME: LocalDateTime = ZonedDateTime.parse(UNIVERSAL_TIME_STRING, DateTimeFormatter.ISO_DATE_TIME)
    .withZoneSameInstant(ZoneOffset.UTC) // Ensures time is interpreted in UTC
    .toLocalDateTime

  def resetProbeAndServer(port: Int)(implicit system: ActorSystem, ec: ExecutionContext, timeout: Timeout): (TestProbe, Server) = {
    val actorSupervisorProbe = TestProbe()
    val server = Server.newStartedAt("localhost", port, actorSupervisorProbe.ref)
    (actorSupervisorProbe, server)
  }

  val mockedGetResponse: ClientStatementResponse = ClientStatementResponse(Client.initialWithId(1).getStatement.copy(
    balanceInformation = Client.initialWithId(1).getStatement.balanceInformation.copy(timestamp = UNIVERSAL_TIME_TIME)
  ))

  val mockedBalanceAndLimitResponse = ClientBalanceAndLimitResponse(100, 0)

  case object MockedTransaction {
    val requestDTO = TransactionRequestDTO(100, "c", "desc")
    val actorCommand = ClientAddTransactionCommand(1, 100, TransactionType.CREDIT, "desc")
  }
  val mockedTransactionRequestDTO = TransactionRequestDTO(100, "c", "desc")
}
