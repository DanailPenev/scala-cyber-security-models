package server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, Cookie}
import akka.http.scaladsl.server.MissingCookieRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class WebServerSpec extends WordSpec with Matchers with ScalatestRouteTest with Routes {
  val models: Vector[String] = Vector("exceptions", "futures")

  "The service" should {
    "return a 'Hello!' response for GET requests to /hello" in {
      // tests:
      Get("/hello") ~> route ~> check {
        responseAs[String] shouldEqual """{"status": "ok"}"""
      }
    }

    "leave GET requests to other paths unhandled" in {
      // tests:
      Get("/kermit") ~> route ~> check {
        handled shouldBe false
      }
    }
  }

  for (model <- models) {
    s"The authentication service for $model" should {
      "authenticate properly with good basic auth params" in {
        val validCredentials = BasicHttpCredentials("User", "ObfuscatedPassword")
        Get(s"/$model/login") ~> addCredentials(validCredentials) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }
      }

      "not authenticate when invalid credentials" in {
        val invalidCredentials = BasicHttpCredentials("John", "p4ssw0rd")
        Get(s"/$model/login") ~> addCredentials(invalidCredentials) ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }

      "not authenticate when no auth provided" in {
        Get(s"/$model/login") ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
    }

    s"The authorization service for $model" should {
      "authorize properly with a good cookie" in {
        Get(s"/$model/admin") ~> Cookie("userName" -> "RandomlyGeneratedCookie") ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }
      }

      "reject if cookie is for non-admin user" in {
        Get(s"/$model/admin") ~> Cookie("userName" -> "FakeCookie") ~> route ~> check {
          status shouldEqual StatusCodes.Forbidden
        }
      }

      "reject if a cookie is missing" in {
        // missing cookie
        Get(s"/$model/admin") ~> route ~> check {
          rejection shouldEqual MissingCookieRejection("userName")
        }
      }
    }
  }

}
