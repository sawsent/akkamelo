package com.akkamelo.api.endpoint.marshalling

import com.akkamelo.api.endpoint.dto._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsValue, RootJsonFormat, deserializationError, enrichAny, serializationError}

trait CustomMarshalling {

  implicit protected val transactionRequestFormat: RootJsonFormat[TransactionRequestDTO] = jsonFormat3(TransactionRequestDTO)

  implicit val balanceFormat: RootJsonFormat[BalanceDTO] = jsonFormat3(BalanceDTO)
  implicit val transactionFormat: RootJsonFormat[TransactionDTO] = jsonFormat4(TransactionDTO)
  implicit val statementResponseFormat: RootJsonFormat[StatementResponseDTOPayload] = jsonFormat2(StatementResponseDTOPayload)
  implicit val transactionResponseFormat: RootJsonFormat[TransactionResponseDTOPayload] = jsonFormat2(TransactionResponseDTOPayload)

  implicit object ResponseDTOPayloadFormat extends RootJsonFormat[Option[ResponseDTOPayload]] {
    def write(payload: Option[ResponseDTOPayload]): JsValue = payload match {
      case Some(payload) => payload match {
        case s: StatementResponseDTOPayload => s.toJson
        case t: TransactionResponseDTOPayload => t.toJson
        case _ => serializationError("Unknown ResponseDTOPayload type")
      }
      case None => JsNull
      case _ => serializationError("Unknown ResponseDTOPayload type")
    }

    def read(value: JsValue): Option[ResponseDTOPayload] =
      deserializationError("Reading ResponseDTOPayload is not supported")
  }
}
