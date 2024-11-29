package com.akkamelo.api.http

import akka.http.scaladsl.model.StatusCode
import com.akkamelo.api.http.dto.{ErrorMessageResponseDTOPayload, RequestDTO, ResponseDTO}

object ClientService {
  def apply(): ClientService = new ClientService()
}

class ClientService {
  def handleRequest(id: Int, requestDTO: RequestDTO): ResponseDTO = {
    println(s"Not implemented yet.")
    ResponseDTO(StatusCode.int2StatusCode(500), Some(ErrorMessageResponseDTOPayload("Not implemented yet.")))
  }
}
