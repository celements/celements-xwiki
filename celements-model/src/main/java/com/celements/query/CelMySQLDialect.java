package com.celements.query;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.dialect.MySQLDialect;

public class CelMySQLDialect extends MySQLDialect {

  public CelMySQLDialect() {
    super();
    registerHibernateType(Types.LONGVARCHAR, Hibernate.TEXT.getName());
  }

}
