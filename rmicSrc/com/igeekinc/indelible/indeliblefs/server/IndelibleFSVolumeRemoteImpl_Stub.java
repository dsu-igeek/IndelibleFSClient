// Stub class generated by rmic, do not edit.
// Contents subject to change without notice.

package com.igeekinc.indelible.indeliblefs.server;

public final class IndelibleFSVolumeRemoteImpl_Stub
    extends java.rmi.server.RemoteStub
    implements com.igeekinc.indelible.indeliblefs.remote.IndelibleFSVolumeRemote, java.rmi.Remote
{
    private static final java.rmi.server.Operation[] operations = {
	new java.rmi.server.Operation("void addSnapshot(com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.DeleteFileInfoRemote deleteObjectByPath(com.igeekinc.util.FilePath)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo getInfoForSnapshot(com.igeekinc.indelible.indeliblefs.core.IndelibleVersion)"),
	new java.rmi.server.Operation("java.util.Map getMetaDataResource(java.lang.String)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote getObjectByID(com.igeekinc.indelible.oid.IndelibleFSObjectID)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote getObjectByPath(com.igeekinc.util.FilePath)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote getObjectForVersion(com.igeekinc.indelible.indeliblefs.core.IndelibleVersion, com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.oid.IndelibleFSObjectID getObjectID()"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote getRoot()"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.core.IndelibleVersion getVersion()"),
	new java.rmi.server.Operation("com.igeekinc.indelible.oid.IndelibleFSObjectID getVolumeID()"),
	new java.rmi.server.Operation("java.lang.String getVolumeName()"),
	new java.rmi.server.Operation("java.lang.String listMetaDataResources()[]"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.IndelibleSnapshotIteratorRemote listSnapshots()"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator listVersions()"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.MoveObjectInfoRemote moveObject(com.igeekinc.util.FilePath, com.igeekinc.util.FilePath)"),
	new java.rmi.server.Operation("void release()"),
	new java.rmi.server.Operation("boolean releaseSnapshot(com.igeekinc.indelible.indeliblefs.core.IndelibleVersion)"),
	new java.rmi.server.Operation("com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote setMetaDataResource(java.lang.String, java.util.Map)"),
	new java.rmi.server.Operation("void setVolumeName(java.lang.String)")
    };
    
    private static final long interfaceHash = 2188626197793157822L;
    
    // constructors
    public IndelibleFSVolumeRemoteImpl_Stub() {
	super();
    }
    public IndelibleFSVolumeRemoteImpl_Stub(java.rmi.server.RemoteRef ref) {
	super(ref);
    }
    
    // methods from remote interfaces
    
    // implementation of addSnapshot(IndelibleSnapshotInfo)
    public void addSnapshot(com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo $param_IndelibleSnapshotInfo_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 0, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_IndelibleSnapshotInfo_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    ref.done(call);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of deleteObjectByPath(FilePath)
    public com.igeekinc.indelible.indeliblefs.remote.DeleteFileInfoRemote deleteObjectByPath(com.igeekinc.util.FilePath $param_FilePath_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException, com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException, com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 1, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_FilePath_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.DeleteFileInfoRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.DeleteFileInfoRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getInfoForSnapshot(IndelibleVersion)
    public com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo getInfoForSnapshot(com.igeekinc.indelible.indeliblefs.core.IndelibleVersion $param_IndelibleVersion_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 2, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_IndelibleVersion_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.core.IndelibleSnapshotInfo) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getMetaDataResource(String)
    public java.util.Map getMetaDataResource(java.lang.String $param_String_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 3, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_String_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    java.util.Map $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (java.util.Map) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getObjectByID(IndelibleFSObjectID)
    public com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote getObjectByID(com.igeekinc.indelible.oid.IndelibleFSObjectID $param_IndelibleFSObjectID_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 4, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_IndelibleFSObjectID_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getObjectByPath(FilePath)
    public com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote getObjectByPath(com.igeekinc.util.FilePath $param_FilePath_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException, com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 5, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_FilePath_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.IndelibleFileNodeRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getObjectForVersion(IndelibleVersion, RetrieveVersionFlags)
    public com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote getObjectForVersion(com.igeekinc.indelible.indeliblefs.core.IndelibleVersion $param_IndelibleVersion_1, com.igeekinc.indelible.indeliblefs.core.RetrieveVersionFlags $param_RetrieveVersionFlags_2)
	throws java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 6, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_IndelibleVersion_1);
		out.writeObject($param_RetrieveVersionFlags_2);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getObjectID()
    public com.igeekinc.indelible.oid.IndelibleFSObjectID getObjectID()
	throws java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 7, interfaceHash);
	    ref.invoke(call);
	    com.igeekinc.indelible.oid.IndelibleFSObjectID $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.oid.IndelibleFSObjectID) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getRoot()
    public com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote getRoot()
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 8, interfaceHash);
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.IndelibleDirectoryNodeRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getVersion()
    public com.igeekinc.indelible.indeliblefs.core.IndelibleVersion getVersion()
	throws java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 9, interfaceHash);
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.core.IndelibleVersion $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.core.IndelibleVersion) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getVolumeID()
    public com.igeekinc.indelible.oid.IndelibleFSObjectID getVolumeID()
	throws java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 10, interfaceHash);
	    ref.invoke(call);
	    com.igeekinc.indelible.oid.IndelibleFSObjectID $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.oid.IndelibleFSObjectID) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getVolumeName()
    public java.lang.String getVolumeName()
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 11, interfaceHash);
	    ref.invoke(call);
	    java.lang.String $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (java.lang.String) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of listMetaDataResources()
    public java.lang.String[] listMetaDataResources()
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 12, interfaceHash);
	    ref.invoke(call);
	    java.lang.String[] $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (java.lang.String[]) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of listSnapshots()
    public com.igeekinc.indelible.indeliblefs.remote.IndelibleSnapshotIteratorRemote listSnapshots()
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 13, interfaceHash);
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.IndelibleSnapshotIteratorRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.IndelibleSnapshotIteratorRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of listVersions()
    public com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator listVersions()
	throws java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 14, interfaceHash);
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.core.IndelibleVersionIterator) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of moveObject(FilePath, FilePath)
    public com.igeekinc.indelible.indeliblefs.remote.MoveObjectInfoRemote moveObject(com.igeekinc.util.FilePath $param_FilePath_1, com.igeekinc.util.FilePath $param_FilePath_2)
	throws com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException, com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException, com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException, com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 15, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_FilePath_1);
		out.writeObject($param_FilePath_2);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.MoveObjectInfoRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.MoveObjectInfoRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.FileExistsException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.NotDirectoryException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.ObjectNotFoundException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of release()
    public void release()
	throws java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 16, interfaceHash);
	    ref.invoke(call);
	    ref.done(call);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of releaseSnapshot(IndelibleVersion)
    public boolean releaseSnapshot(com.igeekinc.indelible.indeliblefs.core.IndelibleVersion $param_IndelibleVersion_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 17, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_IndelibleVersion_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    boolean $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = in.readBoolean();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of setMetaDataResource(String, Map)
    public com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote setMetaDataResource(java.lang.String $param_String_1, java.util.Map $param_Map_2)
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 18, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_String_1);
		out.writeObject($param_Map_2);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote $result;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$result = (com.igeekinc.indelible.indeliblefs.remote.IndelibleFSObjectRemote) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling return", e);
	    } finally {
		ref.done(call);
	    }
	    return $result;
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of setVolumeName(String)
    public void setVolumeName(java.lang.String $param_String_1)
	throws com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException, java.io.IOException, java.rmi.RemoteException
    {
	try {
	    java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 19, interfaceHash);
	    try {
		java.io.ObjectOutput out = call.getOutputStream();
		out.writeObject($param_String_1);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling arguments", e);
	    }
	    ref.invoke(call);
	    ref.done(call);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
}
