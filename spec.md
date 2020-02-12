context: Order-Taking

// ----------------
// Simple types
// ----------------

// Product codes
data WidgetCode = string starting with "W" then 4 digits
data GizmoCode = string starting with "G" then 3 digits
data ProductCode = WidgetCode or GizmoCode

data OrderQuantity = UnitQuantity or KilogramQuantity
data UnitQuantity = integer between 1 and 1000
data KilogramQuantity = decimal between 0.05 and 100.00

// --------------------
// Order life cycle
// --------------------

// ----- unvalidated state -------------
data UnvalidatedOrder =
  UnvalidatedCustomerInfo
  AND UnvalidatedShippingAddress
  AND UnvalidatedBillingAddress
  AND list of UnvalidatedOrderLine

data UnvalidatedOrderLine =
  UnvalidatedProductCode
  AND UnvalidatedOrderQuantity

// ----- validated state ---------------
data ValidatedOrder =
  ValidatedCustomerInfo
  AND ValidatedShippingAddress
  AND ValidatedBillingAddress
  AND list of ValidatedOrderLine

data ValidatedOrderLine =
  ValidatedProductCode
  AND ValidatedOrderQuantity

// ----- priced state ------------------
data PricedOrder =
  ValidatedCustomerInfo
  AND ValidatedShippingAddress
  AND ValidatedBillingAddress
  AND list of PricedOrderLine
  AND AmountToBill

data PricedOrderLine =
  ValidatedOrderLine
  AND LinePrice

// ----- output events -----------------
data OrderAcknowledgementSent = ...
data OrderPlaced = ...
data BillableOrderPlaced = ...
data PlacedOrderAcknowledgement =
  PricedOrder
  AND AcknowledgementLetter


workflow "Place Order" =
  input: UnvalidatedOrder
  output (on success):
    OrderAcknowledgementSent
    AND OrderPlaced event (to send to shipping)
    AND BillableOrderPlaced (to send to billing)
  output (on error):
    InvalidOrder

  // step 1
  do ValidateOrder
  If order is invalid then:
    add InvalidOrder to pile
    stop

  // step 2
  do PriceOrder

  // step 3
  do SendAcknowledgementToCustomer

  // step 4
  return OrderPlaced event (if no errors)


substep "ValidateOrder" =
  input: UnvalidatedOrder
  output: ValidatedOrder OR ValidationError
  dependencies: CheckProductCodeExists, CheckAddressExists

  validate the customer name
  check that the shipping and billing address exist
  for each line:
    check product code syntax
    check that product code exists in ProductCatalog

  if everything is OK, then:
    return ValidatedOrder
  else:
    return ValidationError


substep "PriceOrder" =
  input: ValidatedOrder
  output: PricedOrder
  dependencies: GetProductPrice

  for each line:
    get the price for the product
    set the price for the line
  set the amount to bill ( = sum of the line prices)


substep "SendAcknowledgementToCustomer" =
  input: PricedOrder
  output: None

  create acknowledgement letter and send it
  and the priced oder to the customer

