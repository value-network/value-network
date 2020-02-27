# Value Network

Proof-of-Capacity (PoC) consensus algorithm based blockchain.

### Software Installation

#### Linux (Debian, Ubuntu)

- fetch the newest release ZIP file

If running for the first time,

- install Java
- install MariaDB

#### Windows

###### MariaDb

In the conf directory, copy `val-default.properties` into a new file named `val.properties`.

Download and install MariaDB <https://mariadb.com/downloads/mariadb-tx>

The MariaDb installation will ask to setup a password for the root user. 
Add this password to the `val.properties` file created above in the following section:
```
DB.Url=jdbc:mariadb://localhost:3306/val_master
DB.Username=root
DB.Password=YOUR_PASSWORD
```
