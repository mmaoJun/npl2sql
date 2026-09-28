"""
本地 Ollama + LangChain 调用测试

模型: qwen2.5-coder:7b (通过 Ollama 在本地运行)
框架: langchain-ollama (ChatOllama)
"""
from ftplib import print_line

from langchain_ollama import ChatOllama


def build_llm() -> ChatOllama:
    """创建 LangChain 的 Ollama 聊天模型实例"""
    return ChatOllama(
        model="qwen2.5-coder:7b",
        base_url="http://localhost:11434",
        temperature=0,
    )


def test_basic_invoke(llm: ChatOllama) -> None:
    """测试 1: 基础调用(一次性返回完整回答)"""
    print("=" * 60)
    print("测试 1: 基础调用")
    print("=" * 60)

    prompt = "用一句话解释什么是 NLP2SQL。"
    response = llm.invoke(prompt)

    print(f"提问: {prompt}")
    print(f"回答: {response.content}")
    print()


def test_stream(llm: ChatOllama) -> None:
    """测试 2: 流式输出(逐块返回,模拟打字机效果)"""
    print("=" * 60)
    print("测试 2: 流式输出")
    print("=" * 60)

    prompt = "写一个 Python 快速排序函数并给出调用示例,不要多余解释。"
    print(f"提问: {prompt}")
    print("回答: ", end="", flush=True)

    for chunk in llm.stream(prompt):
        print(chunk.content, end="", flush=True)
    print("\n")


def test_sql_generation(llm: ChatOllama) -> None:
    """测试 3: 自然语言转 SQL(贴近项目核心场景)"""
    print("=" * 60)
    print("测试 3: 自然语言转 SQL")
    print("=" * 60)

    prompt = (
        "表结构: users(id INT, name VARCHAR, age INT, city VARCHAR)\n"
        "问题: 查询年龄大于 25 岁且来自北京的用户姓名,按年龄降序排列。\n"
        "只输出 SQL 语句,不要任何解释。"
    )
    print(f"提问:\n{prompt}\n")
    response = llm.invoke(prompt)
    print(f"回答:\n{response.content}")


if __name__ == "__main__":
    # llm = build_llm()
    # test_basic_invoke(llm)
    # test_stream(llm)
    # test_sql_generation(llm)
    llm = ChatOllama(
        model="qwen2.5-coder:7b",
        base_url="http://localhost:11434",
        temperature=0,
    )
    resp = llm.invoke("你是谁")

    print(resp.content)
    print(resp.response_metadata["model_name"])
    print(resp.usage_metadata["total_tokens"])

