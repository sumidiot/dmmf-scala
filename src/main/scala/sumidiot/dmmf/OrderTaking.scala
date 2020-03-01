package sumidiot.dmmf

import scala.concurrent.Future
import cats.data.NonEmptyList


/**
 * This object represents the beginnings of the implemented version of the domain
 * specification for the Order-Taking domain from the root spec.md.
 */
object OrderTaking {

  sealed trait ProductCode extends Any
  case class WidgetCode(code: String) extends AnyVal with ProductCode // will add constraint later
  case class GizmoCode(code: String) extends AnyVal with ProductCode  // will add constraint later

  sealed trait OrderQuantity extends Any
  /**
   * This was previously a case class, but `private` for those only makes the constructor
   * private, not the associated `apply` method. When we remove the `case` keyword, we are
   * inspired to create an `unapply` so that we can `match`. This does mean we can define
   * the `apply` method in the companion object, where when this was a case class we had
   * no ability to override it with the same argument list.
   *
   * See the following for more:
   *   * https://stackoverflow.com/a/50993017
   *   * https://users.scala-lang.org/t/ending-the-confusion-of-private-case-class-constructor-in-scala-2-13-or-2-14/2915
   */
  class UnitQuantity private (val quantity: Int) extends AnyVal with OrderQuantity
  case class KilogramQuantity(quantity: Double) extends AnyVal with OrderQuantity

  object UnitQuantity {
    def apply(quantity: Int): Either[String, UnitQuantity] =
      if (quantity < 1) {
        Left("UnitQuantity can not be negative")
      } else if (quantity > 1000) {
        Left("UnitQuantity can not be more than 1000")
      } else {
        Right(new UnitQuantity(quantity))
      }

    def unapply(uq: UnitQuantity): Option[Int] =
      Some(uq.quantity)
  }

  type EmailAddress = String
  object EmailVerification {
    /**
     * In the text, it notes that this private constructor would only be called from an
     * email verification service, which I guess would be another module here.
     *
     * Thinking about this asynchronously, you'd email the Unverified address, and wait, taking
     * a note in the database that a verification email was sent. Once it eventually came
     * back, I guess you'd update the database with the verified flat. And then... something like,
     * when you ask for a User, from the database, it would be responsible for checking the
     * database state and creating the CustomerEmail correctly, as either verified or not.
     */
    class VerifiedEmailAddress private[EmailVerification] (email: String)
    type EmailValidationService = EmailAddress => VerifiedEmailAddress
    object EmailValidationService {
      val everythingValidValidatator: EmailValidationService = e => new VerifiedEmailAddress(e)
    }
  }
  sealed trait CustomerEmail extends Any
  object CustomerEmail {
    case class Unverified(emailAddress: EmailAddress) extends AnyVal with CustomerEmail
    case class Verified(emailAddress: EmailVerification.VerifiedEmailAddress) extends AnyVal with CustomerEmail
  }


  type Name = String
  type EmailContactInfo = String
  type PostalContactInfo = String
  sealed trait ContactInfo extends Any
  object ContactInfo {
    case class EmailOnly(email: EmailContactInfo) extends AnyVal with ContactInfo
    case class AddrOnly(address: PostalContactInfo) extends AnyVal with ContactInfo

    /**
     * This elaborates the demonstation of the complication of sealed traits as sum types.
     * First, we end up having to have them be `Any`/`AnyVal`s. Now, for a constructor
     * to take more than one argument, we end up shoving that into its own type, not in the
     * `sealed` hierarchy, and then wrapping that in another `case class` to get it into
     * the hierarchy as an `AnyVal`.
     */
    case class BothContactMethods(email: EmailContactInfo, address: PostalContactInfo)
    case class EmailAndAddr(both: BothContactMethods) extends AnyVal with ContactInfo
  }
  case class Contact(name: Name, info: ContactInfo)

  // placeholder types for ids of "entity" types
  type OrderId = Void
  type OrderLineId = Void
  type CustomerId = Void

  // placeholder types, not sure what a better way to capture these might be
  type CustomerInfo = Void
  type ShippingAddress = Void
  type BillingAddress = Void
  type Price = Void
  type BillingAmount = Void

  case class OrderLine(
    id: OrderLineId,
    orderId: OrderId,
    productCode: ProductCode,
    orderQuantity: OrderQuantity,
    price: Price
  )

  case class UnvalidatedOrder(
    id: OrderId,
    customerId: CustomerId, // aggregate entities should store references to contained entities
    shippingAddress: ShippingAddress,
    billingAddress: BillingAddress,
    orderLines: NonEmptyList[OrderLine],
    amountToBill: BillingAmount
  )

  // more placeholder types
  type ValidatedOrder = Void
  type AcknowledgementSent = Void
  type OrderPlaced = Void
  type BillableOrderPlaced = Void

  case class ValidationError(
    fieldName: String,
    errorDescription: String
  )

  type ValidationResponse[R] = Future[Either[List[ValidationError], R]]
  type ValidateOrder = UnvalidatedOrder => ValidationResponse[ValidatedOrder]

  case class PlaceOrderEvents(
    acknowledgementSent: AcknowledgementSent,
    orderPlaced: OrderPlaced,
    billableOrderPlaced: BillableOrderPlaced
  )

  sealed trait PlaceOrderError
  case class OrderValidationErrors(validationErrors: List[ValidationError])
  type PlaceOrder = UnvalidatedOrder => Either[PlaceOrderError, PlaceOrderEvents]
    
}
