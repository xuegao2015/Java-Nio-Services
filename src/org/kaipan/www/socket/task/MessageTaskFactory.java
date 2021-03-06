package org.kaipan.www.socket.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.kaipan.www.socket.core.Message;
import org.kaipan.www.socket.core.Server;
import org.kaipan.www.socket.core.Socket;

public class MessageTaskFactory implements ITaskFactory
{
	private Class<? extends ITask> TaskClass;
	
	public MessageTaskFactory(Class<? extends ITask> TaskClass) 
	{
		this.TaskClass = TaskClass;
	}
	
	public void setTaskClass(Class<? extends ITask> TaskClass) 
	{
		this.TaskClass = TaskClass;
	}
	
	@Override
	public ITask createTask(Server server, Socket socket, Message message)
	{
		ITask Task = null;
		
		try {
			Class<?>[] classes = new Class[] {
				Server.class,
				Socket.class,
				Message.class
			};
			
			Constructor<? extends ITask> constructor = TaskClass.getConstructor(classes);
			
			Object[] arguments = new Object[] {
				server,
				socket,
				message
			};
			
			Task = (ITask) constructor.newInstance(arguments);
		} 
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return Task;
	}
}
