import ServerSpec.{UNIVERSAL_TIME_TIME, mockedBalanceAndLimitResponse, mockedGetResponse}
import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.KeepRunning
import akka.testkit.TestProbe
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor.{ClientAddTransactionCommand, ClientBalanceAndLimitResponse, ClientGetStatementCommand, ClientStatementResponse}
import com.akkamelo.api.actor.client.domain.state.{Client, TransactionType}
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.ApplyCommand
import com.akkamelo.api.adapter.endpoint.ActorResponse2ResponseDTO
import com.akkamelo.api.endpoint.StartedServer
import com.akkamelo.api.endpoint.dto.{ResponseDTOPayload, TransactionRequestDTO, TransactionResponseDTOPayload}
import com.akkamelo.api.endpoint.marshalling.CustomMarshalling._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import scala.concurrent.duration._


class ServerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec = system.dispatcher

  val mockClientActorSupervisor: TestProbe = TestProbe()

  mockClientActorSupervisor.setAutoPilot((sender: ActorRef, msg: Any) => {
    msg match {
      case ApplyCommand(id, ClientAddTransactionCommand(amount, TransactionType.CREDIT, desc)) =>
        sender ! mockedBalanceAndLimitResponse
      case ApplyCommand(clientId, ClientGetStatementCommand) =>
        sender ! mockedGetResponse
      case _ => println(s"Received message: $msg")
    }
    KeepRunning
  })

  val server: StartedServer = new StartedServer("localhost", 8080, mockClientActorSupervisor.ref)

  "POST /clientes/{clientId}/transacoes" should "process a transaction request successfully" in {
    val transactionRequest = TransactionRequestDTO(100, "c", "desc")

    val transactionRequestJson = transactionRequest.toJson.toString()

    val responseDTO = ActorResponse2ResponseDTO(mockedBalanceAndLimitResponse)

    Post("/clientes/1/transacoes").withEntity(ContentTypes.`application/json`, transactionRequestJson) ~> server.route ~> check {
      mockClientActorSupervisor.expectMsg(ApplyCommand(clientId = 1, ClientAddTransactionCommand(100, TransactionType.CREDIT, "desc")))
      status shouldBe StatusCodes.OK
      responseAs[String] should include (responseDTO.payload.toJson.toString())
    }
  }

  "GET /clientes/{clientId}/extrato" should "return a client statement successfully" in {
    val responseDTO = ActorResponse2ResponseDTO(mockedGetResponse)
    val responsePayloadJsonString = responseDTO.payload.toJson.toString()

    Get("/clientes/1/extrato") ~> server.route ~> check {
      mockClientActorSupervisor.expectMsg(ApplyCommand(1, ClientGetStatementCommand))
      status shouldBe StatusCodes.OK
      responseAs[String] should include (responsePayloadJsonString)
    }
  }
}

object ServerSpec {
  val UNIVERSAL_TIME: String = "2021-01-01T00:00:00Z"
  val UNIVERSAL_TIME_TIME: LocalDateTime = ZonedDateTime.parse(UNIVERSAL_TIME, DateTimeFormatter.ISO_DATE_TIME)
    .withZoneSameInstant(ZoneOffset.UTC) // Ensures time is interpreted in UTC
    .toLocalDateTime

  val mockedGetResponse: ClientStatementResponse = ClientStatementResponse(Client.initialWithId(1).getStatement.copy(
    balanceInformation = Client.initialWithId(1).getStatement.balanceInformation.copy(timestamp = UNIVERSAL_TIME_TIME)
  ))

  val mockedBalanceAndLimitResponse = ClientBalanceAndLimitResponse(100, 0)
}
