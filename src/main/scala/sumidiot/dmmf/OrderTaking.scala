package sumidiot.dmmf

import scala.concurrent.Future

import cats.data.NonEmptyList

import java.time.ZonedDateTime

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
  type UnvalidatedCustomerInfo = Void
  type UnvalidatedShippingAddress = Void
  type UnvalidatedBillingAddress = Void
  type CustomerInfo = Void
  type ShippingAddress = Void
  type BillingAddress = Void
  type Address = Void
  type Price = Double // for now, so that it lets us .sum a List[Price]
  type BillingAmount = Double // again, for now, otherwise the type of .sum of List[Price]

  case class OrderLine(
    id: OrderLineId,
    orderId: OrderId,
    productCode: ProductCode,
    orderQuantity: OrderQuantity,
    price: Price
  )

  type ValidatedOrderLine = OrderLine
  type PricedOrderLine = OrderLine


  /**
   * This implementation is slightly more involved than the one given in the book. Possibly
   * I did something wrong, or maybe it was something swept under the rug in the book. The
   * difference is if the `orderLineId` isn't the id of a line in the order.
   */
  def changeOrderLinePrice(order: Order.Unvalidated, orderLineId: OrderLineId, newPrice: Price): Order.Unvalidated = {
    /**
     * The text doesn't define this method. I've got it here as an inner helper of the outer
     * def, but given the parameter list, probably the book proposes having it ouside, as its
     * own method (making it nicely testable, for example). Also, in the book it uses the
     * function application syntax of `|>`. To better align with that, we'd re-organize this
     * method so that `lines` was taken by itself as a second parameter list.
     */
    def replaceOrderLine(lines: NonEmptyList[OrderLine], orderLineId: OrderLineId, newOrderLine: OrderLine): NonEmptyList[OrderLine] = {
      NonEmptyList(newOrderLine, lines.filter(_.id != orderLineId))
    }
    val orderLine = order.orderLines.find(_.id == orderLineId) // Option[OrderLine]
    val newOrderLine = orderLine.map(_.copy(price = newPrice)) // Option[OrderLine]
    val newOrderLines = newOrderLine.map(replaceOrderLine(order.orderLines, orderLineId, _)).getOrElse(order.orderLines)
    val newAmountToBill = newOrderLines.map(_.price).toList.sum // .sum isn't built in to NEL
    order.copy(orderLines = newOrderLines, amountToBill = newAmountToBill)
  }


  /**
   * We create this sum-type for the various states of Order in the place order workflow,
   * following the book. The claim in the book is that "this is the type that can be persisted
   * to storage or communicated to other contexts."
   *
   * We begin with this a trait extending Any, with the component types as just
   * case classes extending the root abstract type
   */
  sealed abstract class Order
  object Order {
    case class Unvalidated(
      id: OrderId,
      customerInfo: UnvalidatedCustomerInfo,
      shippingAddress: UnvalidatedShippingAddress,
      billingAddress: UnvalidatedBillingAddress,
      orderLines: NonEmptyList[OrderLine],
      amountToBill: BillingAmount
    ) extends Order

    case class Validated(
      orderId: OrderId,
      customerInfo: CustomerInfo,
      shippingAddress: Address,
      billingAddress: Address,
      orderLines: List[ValidatedOrderLine]
    ) extends Order
  
    case class Priced(
      orderId: OrderId,
      customerInfo: CustomerInfo,
      shippingAddress: Address,
      billingAddress: Address,
      orderLines: List[PricedOrderLine],
      amountToBill: BillingAmount
    ) extends Order
  }

  // F# prefers Success on the Left, but we keep the scala (haskell) standard here
  type AsyncResult[Failure, Success] = Future[Either[Failure, Success]]
  
  // more placeholder types
  type AcknowledgementSent = Void
  type OrderPlaced = Order.Priced

  case class ValidationError(
    fieldName: String,
    errorDescription: String
  )

  case class PlaceOrderEvents(
    acknowledgementSent: AcknowledgementSent,
    orderPlaced: OrderPlaced,
    billableOrderPlaced: BillableOrderPlaced
  )

  sealed trait PlaceOrderError
  case class OrderValidationErrors(validationErrors: List[ValidationError])

  // if there are several commands with shared fields, you can abstract out
  // the shared bits and a placeholder for the non-shared bits
  case class Command[Data](
    data: Data,
    timestamp: ZonedDateTime,
    userId: CustomerId
  )
  type PlaceOrderCommand = Command[Order.Unvalidated]
  
  type PlaceOrder = PlaceOrderCommand => Either[PlaceOrderError, PlaceOrderEvents]

  /**
   * Modeling each step in the workflow with types
   */

  /**
   * substep "ValidateOrder" =
   *   input: UnvalidatedOrder
   *   output: ValidatedOrder OR ValidationError
   *   dependencies: CheckProductCodeExists, CheckAddressExists
   * 
   * previously we had set this up as
   *   Order.Unvalidated => Future[Either[List[ValidationError], Order.Validated]]
   */
  type CheckProductCodeExists = ProductCode => Boolean
  type UnvalidatedAddress = Void
  case class CheckedAddress(checked: UnvalidatedAddress) // may change later
  case class AddressValidationError(error: String)
  type CheckAddressExists = UnvalidatedAddress => AsyncResult[AddressValidationError, CheckedAddress]
  type ValidateOrder =
    CheckProductCodeExists
      => CheckAddressExists
      => Order.Unvalidated
      => AsyncResult[ValidationError, Order.Validated]

  /**
   * substep "PriceOrder" =
   *   input: ValidatedOrder
   *   output: PricedOrder
   *   dependencies: GetProductPrice
   */
  case class PricingError(error: String)
  type GetProductPrice = ProductCode => Price
  type PriceOrder =
    GetProductPrice
      => Order.Validated
      => Either[PricingError, Order.Priced]

  /**
   * substep Acknowledge Order
   */
  case class HtmlString(htmlString: String)
  case class OrderAcknowledgement(emailAddress: EmailAddress, letter: HtmlString)
  type CreateOrderAcknowledgementLetter = Order.Priced => HtmlString
  sealed trait SendResult
  object SendResult {
    case object Sent extends SendResult
    case object NotSent extends SendResult
  }
  case class OrderAcknowledgementSent(orderId: OrderId, emailAddress: EmailAddress)
  type SendOrderAcknowledgement = OrderAcknowledgement => Future[SendResult]
  type AcknowledgeOrder =
    CreateOrderAcknowledgementLetter
      => SendOrderAcknowledgement
      => Order.Priced
      => Future[Option[OrderAcknowledgementSent]]


  /**
   * Events to Return
   */

  case class BillableOrderPlaced(orderId: OrderId, billingAddress: Address, amountToBill: BillingAmount)

  sealed abstract class PlaceOrderEvent
  object PlaceOrderEvent {
    case class OrderPlacedEvent(orderPlaced: OrderPlaced) extends PlaceOrderEvent
    case class BillaborOrderPlacedEvent(billableOrderPlaced: BillableOrderPlaced) extends PlaceOrderEvent
    case class OrderAcknowledgementSentEvent(orderAcknowledgementSent: OrderAcknowledgementSent) extends PlaceOrderEvent
  }

  type CreateEvents = Order.Priced => List[PlaceOrderEvent]
}
