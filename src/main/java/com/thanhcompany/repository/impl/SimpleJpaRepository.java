package com.thanhcompany.repository.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.thanhcompany.annotation.Column;
import com.thanhcompany.annotation.Table;
import com.thanhcompany.dto.BuildingDTO;
import com.thanhcompany.mapper.ResultSetMapper;
import com.thanhcompany.repository.EntityManagerFactory;
import com.thanhcompany.repository.JpaRepository;

public class SimpleJpaRepository<T> implements JpaRepository<T> {
	private Class<T> zClass;

	@SuppressWarnings("unchecked")
	public SimpleJpaRepository() {
		Type type = getClass().getGenericSuperclass();
		ParameterizedType parameterizedType = (ParameterizedType) type;
		zClass = (Class<T>) parameterizedType.getActualTypeArguments()[0];

	}

	@Override
	public List<T> findAll(Map<String, Object> params, Object... objects) {
		ResultSetMapper<T> resultSetMapper = new ResultSetMapper<>();
		Connection connection = EntityManagerFactory.getConnection();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String tableName = "";
		if (connection != null) {
			try {
				if (zClass.isAnnotationPresent(Table.class)) {
					Table table = zClass.getAnnotation(Table.class);
					tableName = table.name();

				}
				StringBuilder sql = new StringBuilder("select * from " + tableName + " A where 1=1 ");
				sql = createSQLFindAllCommon(sql, params);
				if (objects != null && objects.length == 1) {
					sql.append(objects[0]);

				}
				statement = connection.prepareStatement(sql.toString());
				resultSet = statement.executeQuery();
				return resultSetMapper.mapRow(resultSet, this.zClass);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
				return new ArrayList<>();
			} finally {
				try {
					if (connection != null) {
						connection.close();

					}
					if (statement != null) {
						statement.close();

					}
					if (resultSet != null) {
						resultSet.close();

					}

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
					return new ArrayList<>();
				}

			}

		}
		return new ArrayList<>();
	}

	protected StringBuilder createSQLFindAllCommon(StringBuilder sql, Map<String, Object> params) {
		if (params != null && params.size() > 0) {
			String[] keys = new String[params.size()];
			Object[] values = new Object[params.size()];
			int i = 0;
			for (Map.Entry<String, Object> item : params.entrySet()) {
				keys[i] = item.getKey();
				values[i] = item.getValue();
				i++;

			}
			for (int i1 = 0; i1 < keys.length; i1++) {
				if (values[i1] instanceof String) {
					if (StringUtils.isNoneBlank(values[i1].toString())) {
						sql.append(" AND LOWER(A." + keys[i1] + ")" + " LIKE LOWER('%" + values[i1].toString() + "%')");
					}

				} else {
					if (values[i1] != null) {
						sql.append(" AND A." + keys[i1] + " = " + values[i1] + "");
					}
				}
			}

		}
		return sql;
	}

	@Override
	public List<T> findAll(String sql, Object... objects) {
		ResultSetMapper<T> resultSetMapper = new ResultSetMapper<>();
		Connection connection = EntityManagerFactory.getConnection();
		// PreparedStatement statement=null;
		Statement statement = null;
		ResultSet resultSet = null;
		if (connection != null) {
			try {
				StringBuilder sqlQuery = new StringBuilder(sql);
				if (objects != null && objects.length == 1) {
					sqlQuery.append(objects[0]);

				}
//				statement=connection.prepareStatement(sql);
//				+6resultSet=statement.executeQuery();
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sqlQuery.toString());
				return resultSetMapper.mapRow(resultSet, this.zClass);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
				return new ArrayList<>();
			} finally {
				try {
					if (connection != null) {
						connection.close();

					}
					if (statement != null) {
						statement.close();

					}
					if (resultSet != null) {
						resultSet.close();

					}

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
					return new ArrayList<>();
				}

			}

		}
		return new ArrayList<>();
	}

	@Override
	public void insert(String sql, Object... objects) {
		Connection connection = null;
		PreparedStatement statement = null;
		connection = EntityManagerFactory.getConnection();
		try {
			connection.setAutoCommit(false);
			statement = connection.prepareStatement(sql);
			int index = 1;
			for (Object object : objects) {
				statement.setObject(index, object);
				index++;

			}
			statement.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}

			}
		} finally {
			try {
				if (connection != null) {
					connection.close();

				}
				if (statement != null) {
					statement.close();

				}

			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				System.out.println(e2.getMessage());

			}
		}

	}

	private String createSQLInsert() {
		String tableName = "";
		if (zClass.isAnnotationPresent(Table.class)) {
			Table table = zClass.getAnnotation(Table.class);
			tableName = table.name();

		}
		StringBuilder fields = new StringBuilder("");
		StringBuilder params = new StringBuilder("");
		for (Field field : zClass.getDeclaredFields()) {
			if (fields.length() >= 1) {
				fields.append(",");
				params.append(",");

			}
			if (field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);
				fields.append(column.name());
				params.append("?");

			}

		}
		Class<?> parentClass = zClass.getSuperclass();
		while (parentClass != null) {
			for (Field field : parentClass.getDeclaredFields()) {
				if (fields.length() >= 1) {
					fields.append(",");
					params.append(",");

				}
				if (field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);
					fields.append(column.name());
					params.append("?");

				}

			}
			parentClass = parentClass.getSuperclass();
		}
		String sql = "INSERT INTO " + tableName + "(" + fields.toString() + ") VALUES(" + params.toString() + ")";
		return sql;
	}

	@Override
	public void insert(Object object) {
		String sql = createSQLInsert();
		Connection connection = null;
		PreparedStatement statement = null;
		connection = EntityManagerFactory.getConnection();
		try {
			connection = EntityManagerFactory.getConnection();
			connection.setAutoCommit(false);
			statement = connection.prepareStatement(sql);
			Class<?> aClass = object.getClass();
			int index = 1;
			for (Field field : aClass.getDeclaredFields()) {
				field.setAccessible(true);
				statement.setObject(index, field.get(object));
				index++;

			}
			Class<?> parentClass = aClass.getSuperclass();
			int indexParent = aClass.getDeclaredFields().length + 1;
			while (parentClass != null) {
				for (Field field : parentClass.getDeclaredFields()) {
					field.setAccessible(true);
					statement.setObject(indexParent, field.get(object));
					indexParent++;

				}
				parentClass = parentClass.getSuperclass();
			}

			statement.executeUpdate();
			connection.commit();
		} catch (SQLException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}

			}
		} finally {
			try {
				if (connection != null) {
					connection.close();

				}
				if (statement != null) {
					statement.close();

				}

			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				System.out.println(e2.getMessage());

			}
		}

	}

	@Override
	public void update(Object object, Object... where) {
		String sql = createSQLUpdate(where);
		Connection connection = null;
		PreparedStatement statement = null;
		connection = EntityManagerFactory.getConnection();
		try {
			connection = EntityManagerFactory.getConnection();
			connection.setAutoCommit(false);
			statement = connection.prepareStatement(sql);
			Class<?> aClass = object.getClass();
			int index = 1;
			for (Field field : aClass.getDeclaredFields()) {
				field.setAccessible(true);
				statement.setObject(index, field.get(object));
				index++;

			}
			Class<?> parentClass = aClass.getSuperclass();
			int indexParent = aClass.getDeclaredFields().length + 1;
			while (parentClass != null) {
				for (Field field : parentClass.getDeclaredFields()) {
					field.setAccessible(true);;
					if(!field.getName().equals("id")) {
						statement.setObject(indexParent, field.get(object));
						indexParent++;
					}
					

				}
				parentClass = parentClass.getSuperclass();
			}
			statement.executeUpdate();
			connection.commit();
		} catch (SQLException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}

			}
		} finally {
			try {
				if (connection != null) {
					connection.close();

				}
				if (statement != null) {
					statement.close();

				}

			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				System.out.println(e2.getMessage());

			}
		}

	}

	private String createSQLUpdate(Object... where) {
		String tableName = "";
		if (zClass.isAnnotationPresent(Table.class)) {
			Table table = zClass.getAnnotation(Table.class);
			tableName = table.name();

		}
		StringBuilder sqlQuery = new StringBuilder("");// COLUMN NAME
		for (Field field : zClass.getDeclaredFields()) {
			if (sqlQuery.length() >= 1) {
				sqlQuery.append(",");

			}
			if (field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);
				sqlQuery.append(column.name() + " = ?");
			}

		}
		Class<?> parentClass = zClass.getSuperclass();
		while (parentClass != null) {
			for (Field field : parentClass.getDeclaredFields()) {
				if (sqlQuery.length() >= 1) {
					sqlQuery.append(",");

				}
				if (field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);
					if(!column.name().equals("id")) {
						sqlQuery.append(column.name() + " = ?");
					}else {
						sqlQuery=sqlQuery.deleteCharAt(sqlQuery.lastIndexOf(","));
					}
				}

			}
			parentClass = parentClass.getSuperclass();
		}
		if (where.length != 0) {
			sqlQuery.append(" WHERE 1 = 2 ");
			for (Object object : where) {
				sqlQuery.append(" or id = " + (Long) object);
			}

		}
		String sql = "UPDATE " + tableName + " SET " + sqlQuery.toString();
		return sql;

	}

	@Override
	public List<T> findById(Object... ids) {
		ResultSetMapper<T> resultSetMapper = new ResultSetMapper<>();
		Connection connection = EntityManagerFactory.getConnection();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String query=createSQLFindById(ids);
		if (connection != null) {
			try {
				statement = connection.prepareStatement(query);
				resultSet = statement.executeQuery();
				return resultSetMapper.mapRow(resultSet, this.zClass);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
				return new ArrayList<>();
			} finally {
				try {
					if (connection != null) {
						connection.close();

					}
					if (statement != null) {
						statement.close();

					}
					if (resultSet != null) {
						resultSet.close();

					}

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
					return new ArrayList<>();
				}

			}

		}
		return new ArrayList<>();
	}
	private String createSQLFindById(Object... ids) {	
		StringBuilder sqlQuery=new StringBuilder("SELECT * FROM building WHERE 1 = 2");
		for(Object id:ids) {
			sqlQuery.append(" OR id = "+id);			
		}
		return sqlQuery.toString();
	}

	@Override
	public void deleteById(Object... ids) {
		Connection connection = null;
		PreparedStatement statement = null;
		connection = EntityManagerFactory.getConnection();
		String sql=createSQLDeleteById(ids);
		try {
			connection.setAutoCommit(false);
			statement = connection.prepareStatement(sql);
			statement.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}

			}
		} finally {
			try {
				if (connection != null) {
					connection.close();

				}
				if (statement != null) {
					statement.close();

				}

			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				System.out.println(e2.getMessage());

			}
		}

		
	}
	String createSQLDeleteById(Object... ids) {
		StringBuilder sqlQuery=new StringBuilder("DELETE FROM building ");
		if(ids.length!=0) {
			sqlQuery.append("WHERE 1 = 2");
			for(Object id:ids) {
				sqlQuery.append(" OR id = "+id);
			}
	
		}
		return sqlQuery.toString();
		
	}

	@Override
	public void deleteRandom(Object object) {
		Connection connection = null;
		PreparedStatement statement = null;
		connection = EntityManagerFactory.getConnection();
		String sql=createSQLDeleteRandom(object);
		try {
			connection.setAutoCommit(false);
			statement = connection.prepareStatement(sql);
			statement.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}

			}
		} finally {
			try {
				if (connection != null) {
					connection.close();

				}
				if (statement != null) {
					statement.close();

				}

			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				System.out.println(e2.getMessage());

			}
		}

		
	}
	private String createSQLDeleteRandom(Object object) {
		String sql="";
		try {
			String tableName = "";
			if (zClass.isAnnotationPresent(Table.class)) {
				Table table = zClass.getAnnotation(Table.class);
				tableName = table.name();
			}
			StringBuilder fields = new StringBuilder("");
			Object value=null;
			for (Field field : zClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);
					field.setAccessible(true);
					value=field.get(object);
					if(value!=null) {
						if(value instanceof String && StringUtils.isNotEmpty(value.toString())) {
							fields.append(" AND LOWER("+column.name()+") = LOWER('"+field.get(object)+"')");		
							
						}else {
							
							fields.append(" AND "+column.name()+" = "+field.get(object));		
						}
						
					}
				}

			}
			Class<?> parentClass = zClass.getSuperclass();
		
			while (parentClass != null) {
				for (Field field : parentClass.getDeclaredFields()) {
					Column column = field.getAnnotation(Column.class);
					field.setAccessible(true);
					value=field.get(object);
					if(value!=null) {
						if(value instanceof String && StringUtils.isNotEmpty(value.toString())) {
							
							fields.append(" AND LOWER("+column.name()+") = LOWER('"+field.get(object)+"')");	
						}else {
							
							fields.append(" AND "+column.name()+" = "+field.get(object));		
						}
						
					}
					

				}
				parentClass = parentClass.getSuperclass();
			}
			sql = "DELETE FROM "+tableName+" WHERE 1 = 1" + fields.toString();
		
			
			
			
		} 
		catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sql;
	}
}
