package sumidiot.dmmf

import scala.concurrent.Future


/**
 * This object represents the beginnings of the implemented version of the domain
 * specification for the Order-Taking domain from the root spec.md.
 */
object OrderTaking {

  sealed trait ProductCode extends Any
  case class WidgetCode(code: String) extends AnyVal with ProductCode // will add constraint later
  case class GizmoCode(code: String) extends AnyVal with ProductCode  // will add constraint later

  sealed trait OrderQuantity extends Any
  case class UnitQuantity(quantity: Int) extends AnyVal with OrderQuantity
  case class KilogramQuantity(quantity: Double) extends AnyVal with OrderQuantity

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
    orderLines: List[OrderLine],
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
