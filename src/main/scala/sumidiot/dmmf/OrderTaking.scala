package sumidiot.dmmf


/**
 * This object represents the beginnings of the implemented version of the domain
 * specification for the Order-Taking domain from the root spec.md.
 */
object OrderTaking {

  sealed trait ProductCode extends Any
  case class WidgetCode(code: String) extends AnyVal with ProductCode
  case class GizmoCode(code: String) extends AnyVal with ProductCode

  sealed trait OrderQuantity extends Any
  case class UnitQuantity(quantity: Int) extends AnyVal with OrderQuantity
  case class KilogramQuantity(quantity: Double) extends AnyVal with OrderQuantity

  type CustomerId = Int
  type OrderId = Int

  // placeholder types, not sure what a better way to capture these might be
  type CustomerInfo = Void
  type ShippingAddress = Void
  type BillingAddress = Void
  type OrderLine = Void

  case class UnvalidatedOrder(
    customerInfo: CustomerInfo,
    shippingAddress: ShippingAddress,
    billingAddress: BillingAddress,
    orderLines: List[OrderLine]
  )
    
}
