

# **🌟 Gold Inventory Management System**

### *Smart, Secure & Modern Java Swing Application for Jewelry Businesses*

<p align="center">
  <img src="https://img.shields.io/badge/Java-11+-red?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-Build-blue?logo=apachemaven" />
  <img src="https://img.shields.io/badge/MySQL-8.0-orange?logo=mysql" />
  <img src="https://img.shields.io/github/last-commit/navin-oss/gold-inventory-management?color=brightgreen" />
  <img src="https://img.shields.io/badge/License-MIT-green" />
</p>

---

## **✨ Overview**

The **Gold Inventory Management System** is a full-featured desktop application built with
**Java Swing + MySQL + Apache POI**, designed for jewelry stores to automate inventory, sales, authentication, and reporting.

It features:

* Smooth UI
* Secure login (SHA-256)
* Role-based dashboards
* Excel report automation
* Modular architecture

This README includes structured information from the submitted PDF.


---

# **🎯 Highlighted Features**

### **🔐 Secure Authentication**

* SHA-256 hashed passwords
* Role-based access: **Admin**, **Staff**, **Customer**
* Login, registration, and validation screens

### **📊 Inventory & Sales**

* Add, edit, delete gold items (weight, purity, price/gram)
* Auto total price calculation
* Customer purchase tracking
* Sales + purchase history table
* Excel export of daily reports

### **📦 Excel Report Automation**

Built using **Apache POI**, exporting data like:

```
Sale ID | Customer ID | Item | Weight | Purity | Amount | Date
```

### **🎨 Modern UI**

* Navy + Gold premium color theme
* Animations (welcome screen bubbles)
* Smooth buttons
* Clean tables and dashboard tabs

---

# **🖼 UI Previews**

> (Already uploaded screenshots will render automatically if file names match)

### **🔹 Welcome Screen**

![Welcome](Screenshot%202025-12-07%20130527.png)

### **🔹 Create Account**

![Create Account](Screenshot%202025-12-07%20130637.png)

### **🔹 Customer Dashboard**

![Dashboard](Screenshot%202025-12-07%20130737.png)

---



This makes your repo look *super premium*.

---

# **🧱 Architecture**

```
gold-inventory-system/
│
├── com.goldinventory
│   ├── GoldInventoryManagementSystem.java   # Entry point
│   ├── database/DBConnection.java           # MySQL connector
│   ├── service/AuthService.java             # Login & hashing
│   ├── service/ExcelExporter.java           # Excel reports
│   └── ui/
│       ├── LoginFrame.java
│       ├── admin/AdminDashboardFrame.java
│       └── customer/CustomerDashboardFrame.java
│
└── pom.xml
```

---

# **⚙️ Technologies**

| Type       | Tool            |
| ---------- | --------------- |
| Language   | Java 11+        |
| Database   | MySQL 8.0       |
| UI         | Java Swing      |
| Build Tool | Maven           |
| Excel      | Apache POI      |
| Security   | SHA-256 Hashing |

---

# **🚀 Setup Guide**

### **1️⃣ Install Requirements**

* MySQL Server
* Java JDK 11+
* Maven
* Eclipse/IntelliJ

### **2️⃣ Create Database**

```sql
CREATE DATABASE gold_inventory_db;
```

### **3️⃣ Configure DB**

Update `DBConnection.java` with your MySQL credentials.

### **4️⃣ Run**

`Run → GoldInventoryManagementSystem.java`

---

# **🔑 Default Logins**

| Role     | Username  | Password    |
| -------- | --------- | ----------- |
| Admin    | admin     | admin       |
| Staff    | staff1    | staff123    |
| Customer | customer1 | customer123 |

---

# **🛡 Security Features**

* SHA-256 password hashing
* SQL Injection protection via PreparedStatement
* Role-based UI locking
* Validations for all inputs
* Database foreign key constraints

---

# **📚 References**

1. Apache POI
2. Oracle Java Docs
3. MySQL Connector/J
4. Swing UI Guides

---

# **👥 Team**

### **Team GOLD**

* **Navin** — Backend, Auth, Sales Logic, DB Design
* **Sarthak** — Swing UI & Dashboard
* **Prasenjeet** — Testing, Excel Export, Documentation

(As documented in PDF)


---

# **🏷 Suggested GitHub Tags**

```
java
swing
mysql
inventory-management
desktop-app
apache-poi
excel-export
jdbc
oop
java-gui
management-system
```


