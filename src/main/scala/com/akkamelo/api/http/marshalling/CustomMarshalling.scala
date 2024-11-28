package com.akkamelo.api.http.marshalling

import com.akkamelo.api.http.dto._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsValue, RootJsonFormat, deserializationError, enrichAny, serializationError}

object CustomMarshalling {

  implicit val transactionRequestFormat: RootJsonFormat[TransactionRequestDTO] = jsonFormat3(TransactionRequestDTO)

  implicit val balanceFormat: RootJsonFormat[BalanceDTO] = jsonFormat3(BalanceDTO)
  implicit val transactionFormat: RootJsonFormat[TransactionDTO] = jsonFormat4(TransactionDTO)
  implicit val statementResponseFormat: RootJsonFormat[StatementResponseDTOPayload] = jsonFormat2(StatementResponseDTOPayload)
  implicit val transactionResponseFormat: RootJsonFormat[TransactionResponseDTOPayload] = jsonFormat2(TransactionResponseDTOPayload)
  implicit val errorMessageResponseDTOPayload: RootJsonFormat[ErrorMessageResponseDTOPayload] = jsonFormat1(ErrorMessageResponseDTOPayload)

  implicit object ResponseDTOPayloadFormat extends RootJsonFormat[Option[ResponseDTOPayload]] {
    def write(payload: Option[ResponseDTOPayload]): JsValue = payload match {
      case Some(payload) => payload match {
        case s: StatementResponseDTOPayload => s.toJson
        case t: TransactionResponseDTOPayload => t.toJson
        case e: ErrorMessageResponseDTOPayload => e.toJson
        case _ => serializationError("Unknown ResponseDTOPayload type")
      }
      case None => JsNull
      case _ => serializationError("Unknown ResponseDTOPayload type")
    }

    def read(value: JsValue): Option[ResponseDTOPayload] =
      deserializationError("Reading ResponseDTOPayload is not supported")
  }
}