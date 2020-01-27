package sumidiot.dmmf


/**
 * This object represents the beginnings of the implemented version of the domain
 * specification for the Order-Taking domain from the root spec.md.
 */
object OrderTaking {

  // compare F#, type ProductCode = ProductCode of string
  type ProductCode = String // this will be replaced by WidgetCode or GizmoCode at some point

  // compare F#, type Person = {First:string, Last:string}
  case class Person(
    first: String,
    middleInitial: Option[String],
    last: String
  )

  /**
   * This example, in F# in the book, is written more briefly as
   *   type OrderQuantity =
   *     | UnitQuantity of int
   *     | KilogramQuantity of double
   * 
   * I'm not sure how to get to a representation here in scala that's closer to that.
   */
  sealed trait OrderQuantity
  case class UnitQuantity(value: Int) extends OrderQuantity
  case class KilogramQuantity(value: Double) extends OrderQuantity


  type CheckNumber = Int
  type CardNumber = String

  /**
   * This notation is briefer in F#, in the book, as
   *   type CardType = Visa | Mastercard
   */
  sealed trait CardType
  case object Visa extends CardType
  case object Mastercard extends CardType

  case class CreditCardInfo(cardType: CardType, cardNumber: CardNumber)

  sealed trait PaymentMethod
  case object Cash extends PaymentMethod
  case class Check(checkNumber: CheckNumber) extends PaymentMethod
  case class Card(cardInfo: CreditCardInfo) extends PaymentMethod

  type PaymentAmount = Double
  sealed trait Currency
  case object EUR extends Currency
  case object USD extends Currency

  case class Payment(
    amount: PaymentAmount,
    currency: Currency,
    method: PaymentMethod
  )

  type UnpaidInvoice = Object // would rather put ??? here, but that doesn't work
  type PaidInvoice = Object // again, will have to specify this later

  sealed trait PaymentError
  case object CardTypeNotRecognized
  case object PaymentRejected
  case object PaymentProviderOffline

  type PayInvoice = UnpaidInvoice => Payment => Either[PaymentError, PaidInvoice]

  type ConvertPaymentCurrency = Payment => Currency => Payment

}
