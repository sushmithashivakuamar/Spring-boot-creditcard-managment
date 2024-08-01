## Models
Following is a component overview of the models. 
- `User`: each user has their name, and some (or none) credit cards associated with them
- `CreditCard`: each credit card has issance bank, card number, who the card belongs to, and a list of balance history
- `BalanceHistory`: credit card balance for a specific date.

## Useful Tools
- **PostMan**: useful to send http requests to test your API
- **H2 Console**: when running your project, you can use `http://localhost:8080/h2-ui` to access the h2 console. This will allow to look at what's stored in the database.
  - The database name is `database`
  - The username is `sa`
  - The password is `password`

