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
package org.entando.entando.aps.system.services.api.model;

import com.agiletec.aps.util.ApsProperties;
import java.io.Serializable;

/**
 * The rappresentation of a service
 * @author E.Santoboni
 */
public class ApiService implements Serializable {
	
	protected ApiService() {}
	
	public ApiService(String key, ApsProperties description, ApiMethod master, ApsProperties parameters, 
                        String[] freeParameters, String tag, boolean isPublic, boolean isActive, boolean isMyEntando) {
		this.setKey(key);
		this.setDescription(description);
		this.setMaster(master);
		this.setParameters(parameters);
		this.setFreeParameters(freeParameters);
		this.setTag(tag);
		this.setPublicService(isPublic);
		this.setActive(isActive);
                this.setMyEntando(isMyEntando);
	}
	
	@Override
	public ApiService clone() {
		ApiService clone = new ApiService();
		clone.setDescription(this.getDescription());
		if (null != this.getFreeParameters()) {
			String[] freeParameters = new String[this.getFreeParameters().length];
			for (int i = 0; i < this.getFreeParameters().length; i++) {
				freeParameters[i] = this.getFreeParameters()[i];
			}
			clone.setFreeParameters(freeParameters);
		}
		clone.setKey(this.getKey());
		clone.setMaster(this.getMaster().clone());
		if (null != this.getParameters()) {
			clone.setParameters(this.getParameters().clone());	
		}
		clone.setTag(this.getTag());
		clone.setPublicService(this.isPublicService());
		clone.setActive(this.isActive());
		clone.setMyEntando(this.isMyEntando());
		return clone;
	}
	
	public String getKey() {
		return _key;
	}
	protected void setKey(String key) {
		this._key = key;
	}
	
	public ApsProperties getDescription() {
		return _description;
	}
	protected void setDescription(ApsProperties description) {
		this._description = description;
	}
	
	public ApsProperties getParameters() {
		return _parameters;
	}
	public void setParameters(ApsProperties parameters) {
		this._parameters = parameters;
	}
	
	public String[] getFreeParameters() {
		return _freeParameters;
	}
	protected void setFreeParameters(String[] freeParameters) {
		this._freeParameters = freeParameters;
	}
	public boolean isFreeParameter(String paramName) {
		if (null == this.getFreeParameters() || null == paramName) return false;
		for (int i = 0; i < this.getFreeParameters().length; i++) {
			String parameter = this.getFreeParameters()[i];
			if (parameter.equals(paramName)) return true;
		}
		return false;
	}
	
	public ApiMethod getMaster() {
		return _master;
	}
	protected void setMaster(ApiMethod master) {
		this._master = master;
	}
	
	public String getTag() {
		return _tag;
	}
	protected void setTag(String tag) {
		this._tag = tag;
	}
	
	public boolean isPublicService() {
		return _publicService;
	}
	public void setPublicService(boolean publicService) {
		this._publicService = publicService;
	}
	
	public boolean isActive() {
		return _active;
	}
	public void setActive(boolean active) {
		this._active = active;
	}
        
        public boolean isMyEntando() {
            return _myEntando;
        }
        protected void setMyEntando(boolean myEntando) {
            this._myEntando = myEntando;
        }
	
	private String _key;
	private ApsProperties _description;
	private ApiMethod _master;
	private ApsProperties _parameters;
	private String[] _freeParameters;
	
	private String _tag;
	
	private boolean _publicService;
	private boolean _active;
        private boolean _myEntando;
	
}