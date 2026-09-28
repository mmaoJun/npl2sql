"""
本地 Ollama + LangChain 调用测试

模型: qwen2.5-coder:7b (通过 Ollama 在本地运行)
框架: langchain-ollama (ChatOllama)
"""

from langchain_ollama import ChatOllama

if __name__ == "__main__":

    """
    本地模型调用
    模型: qwen2.5:7b, codellama:7b, qwen2.5-coder:7b 
    """

    llm = ChatOllama(
        model="qwen2.5:7b",
        base_url="http://localhost:11434",
        temperature=0,
    )
    resp = llm.invoke("你是谁，你能干嘛")
    print(resp.content)
    print(resp.response_metadata["model_name"])


