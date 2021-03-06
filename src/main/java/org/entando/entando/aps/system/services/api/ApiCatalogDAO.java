/*
*
* Copyright 2012 Entando S.r.l. (http://www.entando.com) All rights reserved.
*
* This file is part of Entando software.
* Entando is a free software; 
* you can redistribute it and/or modify it
* under the terms of the GNU General Public License (GPL) as published by the Free Software Foundation; version 2.
* 
* See the file License for the specific language governing permissions   
* and limitations under the License
* 
* 
* 
* Copyright 2012 Entando S.r.l. (http://www.entando.com) All rights reserved.
*
*/
package org.entando.entando.aps.system.services.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.aps.system.services.api.model.ApiMethod.HttpMethod;
import org.entando.entando.aps.system.services.api.model.ApiResource;
import org.entando.entando.aps.system.services.api.model.ApiService;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.common.AbstractDAO;
import com.agiletec.aps.util.ApsProperties;

/**
 * @author E.Santoboni
 */
public class ApiCatalogDAO extends AbstractDAO implements IApiCatalogDAO {

    public void loadApiStatus(Map<String, ApiResource> resources) {
        Connection conn = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(LOAD_API_STATUS);
            //resource, httpmethod, isactive, authenticationrequired, authorizationrequired
            //"SELECT method, isactive FROM apicatalog_status";
            res = stat.executeQuery();
            while (res.next()) {
                String resourceCode = res.getString("resource");
                String httpMethodString = res.getString("httpmethod");
                ApiMethod.HttpMethod httpMethod = Enum.valueOf(ApiMethod.HttpMethod.class, httpMethodString.toUpperCase());
                ApiMethod method = null;
                ApiResource resource = resources.get(resourceCode);
                if (null != resource) {
                    method = resource.getMethod(httpMethod);
                }
                if (null == method) {
                    this.resetApiStatus(resourceCode, httpMethod, conn);
                    continue;
                }
                boolean active = (res.getInt("isactive") == 1);
                method.setStatus(active);
                boolean authenticationRequired = (res.getInt("authenticationrequired") == 1);
                method.setRequiredAuth(authenticationRequired);
                String requiredPermission = res.getString("authorizationrequired");
                if (null != requiredPermission && requiredPermission.trim().length() > 0) {
                    method.setRequiredPermission(requiredPermission);
                } else {
                    method.setRequiredPermission(null);
                }
            }
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            processDaoException(t, "Error while loading api status ", "loadApiStatus");
        } finally {
            closeDaoResources(res, stat, conn);
        }
    }

    public void resetApiStatus(String resourceCode, HttpMethod httpMethod) {
        Connection conn = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            this.resetApiStatus(resourceCode, httpMethod, conn);
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            processDaoException(t, "Error resetting status : resource '"
                    + resourceCode + "' method " + httpMethod.toString(), "resetApiStatus");
        } finally {
            closeConnection(conn);
        }
    }

    protected void resetApiStatus(String resourceCode, HttpMethod httpMethod, Connection conn) {
        PreparedStatement stat = null;
        try {
            stat = conn.prepareStatement(RESET_API_STATUS);
            stat.setString(1, resourceCode);
            stat.setString(2, httpMethod.toString());
            stat.executeUpdate();
        } catch (Throwable t) {
            processDaoException(t, "Error resetting status : resource '"
                    + resourceCode + "' method " + httpMethod.toString(), "resetApiStatus");
        } finally {
            closeDaoResources(null, stat);
        }
    }

    public void saveApiStatus(ApiMethod method) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
			String resourceCode = ApiResource.getCode(method.getNamespace(), method.getResourceName());
            this.resetApiStatus(resourceCode, method.getHttpMethod(), conn);
            stat = conn.prepareStatement(SAVE_API_STATUS);
            //resource, httpmethod, isactive, authenticationrequired, authorizationrequired
            int isActive = (method.isActive()) ? 1 : 0;
            stat.setString(1, resourceCode);
            stat.setString(2, method.getHttpMethod().toString());
            stat.setInt(3, isActive);
            int authentication = (method.getRequiredAuth()) ? 1 : 0;
            stat.setInt(4, authentication);
            stat.setString(5, method.getRequiredPermission());
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            processDaoException(t, "Error while saving api status", "saveApiStatus");
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }
	
    @Deprecated
    public Map<String, ApiService> loadServices(Map<String, ApiMethod> methods) {
        return this.loadServices(new ArrayList<ApiMethod>(methods.values()));
    }
	
    public Map<String, ApiService> loadServices(List<ApiMethod> methods) {
        Map<String, ApiMethod> methodMap = new HashMap<String, ApiMethod>();
        for (int i = 0; i < methods.size(); i++) {
            ApiMethod method = methods.get(i);
			String resourceCode = ApiResource.getCode(method.getNamespace(), method.getResourceName());
			methodMap.put(resourceCode, method);
        }
        Map<String, ApiService> services = new HashMap<String, ApiService>();
        Connection conn = null;
        Statement stat = null;
        ResultSet res = null;
        List<String> invalidServices = new ArrayList<String>();
        try {
            conn = this.getConnection();
            stat = conn.createStatement();
            res = stat.executeQuery(LOAD_SERVICES);
            //servicekey, parentapi, description, parameters, 
            //tag, freeparameters, isactive, ispublic
            while (res.next()) {
                this.buildService(methodMap, services, invalidServices, res);
            }
        } catch (Throwable t) {
            processDaoException(t, "Error while loading services", "loadServices");
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return services;
    }
	
    private void buildService(Map<String, ApiMethod> methods,
            Map<String, ApiService> services, List<String> invalidServices, ResultSet res) {
        String key = null;
        try {
            key = res.getString(1);
            String parentCode = res.getString(2);
            ApiMethod masterMethod = methods.get(parentCode);
            if (null != masterMethod) {
                ApsProperties description = new ApsProperties();
                description.loadFromXml(res.getString(3));
                ApsProperties parameters = new ApsProperties();
                parameters.loadFromXml(res.getString(4));
                String tag = res.getString(5);
                String[] freeParameters = null;
                String freeParamString = res.getString(6);
                if (null != freeParamString && freeParamString.trim().length() > 0) {
                    ServiceExtraConfigDOM dom = new ServiceExtraConfigDOM(freeParamString);
                    freeParameters = dom.extractFreeParameters();
                }
                boolean isActive = (1 == res.getInt(7)) ? true : false;
                boolean isPublic = (1 == res.getInt(8)) ? true : false;
                boolean isMyEntando = (1 == res.getInt(9)) ? true : false;
                ApiService apiService = new ApiService(key, description, masterMethod,
                        parameters, freeParameters, tag, isPublic, isActive, isMyEntando);
                services.put(key, apiService);
            } else {
                invalidServices.add(key);
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "buildService", "Error building service - key '" + key + "'");
        }
    }

    public void addService(ApiService service) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(ADD_SERVICE);
            //servicekey, resource, description, parameters, tag, freeparameters, isactive, ispublic
            stat.setString(1, service.getKey());
			String resourceCode = ApiResource.getCode(service.getMaster().getNamespace(), service.getMaster().getResourceName());
			stat.setString(2, resourceCode);
            stat.setString(3, service.getDescription().toXml());
            stat.setString(4, service.getParameters().toXml());
            stat.setString(5, service.getTag());
            if (null == service.getFreeParameters() || service.getFreeParameters().length == 0) {
                stat.setNull(6, Types.VARCHAR);
            } else {
                ServiceExtraConfigDOM dom = new ServiceExtraConfigDOM();
                stat.setString(6, dom.extractXml(service.getFreeParameters()));
            }
            int isActive = (service.isActive()) ? 1 : 0;
            stat.setInt(7, isActive);
            int isPublic = (service.isPublicService()) ? 1 : 0;
            stat.setInt(8, isPublic);
            int isMyEntando = (service.isMyEntando()) ? 1 : 0;
            stat.setInt(9, isMyEntando);
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            processDaoException(t, "Error while adding a service", "addService");
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    public void updateService(ApiService service) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(UPDATE_SERVICE);
            //SET resource = ? , description = ? , parameters = ? , tag = ? , freeparameters = ? , isactive = ? , ispublic = ? WHERE servicekey = ? ";
            String resourceCode = ApiResource.getCode(service.getMaster().getNamespace(), service.getMaster().getResourceName());
			stat.setString(1, resourceCode);
            stat.setString(2, service.getDescription().toXml());
            stat.setString(3, service.getParameters().toXml());
            stat.setString(4, service.getTag());
            if (null == service.getFreeParameters() || service.getFreeParameters().length == 0) {
                stat.setNull(5, Types.VARCHAR);
            } else {
                ServiceExtraConfigDOM dom = new ServiceExtraConfigDOM();
                stat.setString(5, dom.extractXml(service.getFreeParameters()));
            }
            int isActive = (service.isActive()) ? 1 : 0;
            stat.setInt(6, isActive);
            int isPublic = (service.isPublicService()) ? 1 : 0;
            stat.setInt(7, isPublic);
            int isMyEntando = (service.isMyEntando()) ? 1 : 0;
            stat.setInt(8, isMyEntando);
            stat.setString(9, service.getKey());
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            processDaoException(t, "Error while updating a service", "updateService");
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    public void deleteService(String key) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(DELETE_SERVICE);
            stat.setString(1, key);
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            processDaoException(t, "Error while deleting a service", "deleteService");
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }
    
    private static final String LOAD_API_STATUS =
            "SELECT resource, httpmethod, isactive, authenticationrequired, authorizationrequired "
            + "FROM apicatalog_methods";
    
    private static final String SAVE_API_STATUS =
            "INSERT INTO apicatalog_methods(resource, httpmethod, isactive, "
            + "authenticationrequired, authorizationrequired) VALUES (?, ?, ?, ?, ?)";
    
    private static final String RESET_API_STATUS =
            "DELETE FROM apicatalog_methods WHERE resource = ? AND httpmethod = ?";
    
    private static final String LOAD_SERVICES =
            "SELECT servicekey, resource, description, parameters, tag, "
            + "freeparameters, isactive, ispublic, myentando FROM apicatalog_services";
    
    private static final String ADD_SERVICE =
            "INSERT INTO apicatalog_services(servicekey, resource, "
            + "description, parameters, tag, freeparameters, isactive, ispublic, myentando) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? ) ";
    
    private static final String UPDATE_SERVICE =
            "UPDATE apicatalog_services SET resource = ? , description = ? , parameters = ? , tag = ? , freeparameters = ? , isactive = ? , ispublic = ? , myentando = ? WHERE servicekey = ? ";
    
    private static final String DELETE_SERVICE =
            "DELETE FROM apicatalog_services WHERE servicekey = ? ";
    
}
