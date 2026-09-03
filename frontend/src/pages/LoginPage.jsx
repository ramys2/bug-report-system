import LoginForm from "../components/LoginForm";

function LoginPage() {
    return (
        <>
            <main className="container d-flex flex-grow-1 align-items-center justify-content-center py-5">
                <div className="col-12 col-sm-8 col-md-6 col-lg-5">
                    <LoginForm />
                </div>
            </main>
        </>
    );
}

export default LoginPage;
